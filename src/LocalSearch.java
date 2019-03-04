import java.util.*;

public class LocalSearch extends Heuristics {
    public static void main(String[] args, int[] p) {
        int T = shifts * weeks * days * rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

        int[] delta = new int[T];
        for (int i = 0; i < T / 2; i++) {
            delta[i] = delta1;
        }
        for (int i = T / 2; i < T; i++) {
            delta[i] = delta2;
        }
        //в качестве единиц измерения берем минуты
        int[] A = setSlotBounds(T, true); //начальные моменты интервалов
        int[] B = setSlotBounds(T, false); //конечные моменты интервалов

        int[] c = setCostPerUnitTimeOfExtension(T); //стоимость единицы времени увеличения
        int G = weeks * days; //кол-во групп

        int[] n = setNumberOfPatientsInGroups(G); //кол-во пациентов в группах
        int m = getTotalNumberOfPatients(n); //кол-во операций
        int[] patientsAndGroups = getGroupsForPatients(m, n);
        System.out.println("Кол-во операций m = " + m);

        int[] r = setReadyDates(G, A); //сроки готовности для группы операций
        int[] d = setDueDates(r); //конечные сроки

        double[] w = setOperationsWeights(n); //веса групп операций

        //int[] p = setOperationsTimes(m);

        double objectiveFunction = 0;
        for (int group = 0; group < G; group++) {

            //начальная популяция
            int numberOfSolutions = 100000;
            Random random = new Random();
            List<List<Integer>> solutions = new ArrayList<>(numberOfSolutions);
            for (int i = 0; i < numberOfSolutions; i++) {
                solutions.add(new ArrayList<>(n[group]));
            }

            //генерация случайных решений
            for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
                for (int i = 0; i < n[group]; i++) {
                    int slot = random.nextInt(rooms * shifts);
                    boolean assign = random.nextBoolean();
                    solutions.get(cnt).add(assign ? slot : -1);
                }
            }

            double bestCost = -1;
            int bestIndex = -1;
            List<Integer> bestSolution;

            int[] deltaForGroup = new int[rooms * shifts];
            Arrays.fill(deltaForGroup, 0, rooms * shifts / 2, delta1);
            Arrays.fill(deltaForGroup,  rooms * shifts / 2, rooms * shifts, delta2);
            int[] AForGroup = new int[rooms * shifts];
            int[] BForGroup = new int[rooms * shifts];
            int[] cForGroup = new int[rooms * shifts];
            int index = 0;
            for (int i = 0; i < T; i++) {
                if (A[i] == r[group] || B[i] == d[group]) {
                    AForGroup[index] = A[i];
                    BForGroup[index] = B[i];
                    cForGroup[index++] = c[i];
                }
            }
            index = 0;
            int[] pForGroup = new int[n[group]];
            for (int i = 0; i < m; i++) {
                if (getGroupByIndex(i, patientsAndGroups) == group) {
                    pForGroup[index++] = p[i];
                }
            }
            //evaluate
            for (int i = 0; i < solutions.size(); i++) {
                double cost = calculateFitnessFunction(rooms * shifts, deltaForGroup, r[group], AForGroup, BForGroup, cForGroup,
                        n[group], d[group], w[group], pForGroup, solutions.get(i));
                if (cost > bestCost) {
                    bestCost = cost;
                    bestIndex = i;
                    System.out.println("Best cost " + bestCost + " iteration " + 1 + solutions.get(bestIndex));
                }
            }

            for (int cnt = 0; cnt < 100; cnt++) {
                bestSolution = new ArrayList<>();
                for (int i = 0; i < n[group]; i++) {
                    bestSolution.add(0);
                }
                Collections.copy(bestSolution, solutions.get(bestIndex));
                //выбираем родителей
                for (List<Integer> solution : solutions) {
                    int pr = random.nextInt(100);
                    if (pr < 50) {
                        int index1 = random.nextInt(numberOfSolutions);
                        int index2 = random.nextInt(numberOfSolutions);
                        while (index1 == index2) {
                            index2 = random.nextInt(numberOfSolutions);
                        }
                        //скрещивание
                        List<Integer> child1 = crossover(solutions.get(index2));
                        List<Integer> child2 = crossover(solutions.get(index1));
                        Collections.copy(solutions.get(index1), child1);
                        Collections.copy(solutions.get(index2), child2);
                    }
                }

                //поставить куда-нибудь лучшее решение
                index = random.nextInt(numberOfSolutions);
                while (index == bestIndex) {
                    index = random.nextInt(numberOfSolutions);
                }
                Collections.copy(solutions.get(index), bestSolution);

                //evaluate
                for (int i = 0; i < solutions.size(); i++) {
                    double cost = calculateFitnessFunction(rooms * shifts, deltaForGroup, r[group], AForGroup, BForGroup,
                            cForGroup, n[group], d[group], w[group], pForGroup, solutions.get(i));
                    if (cost > bestCost) {
                        bestCost = cost;
                        bestIndex = i;
                        System.out.println("Best cost " + bestCost + " iteration " + cnt + solutions.get(bestIndex));
                    }
                }
                //отбор
                selection(solutions, rooms * shifts, deltaForGroup, r[group], AForGroup, BForGroup, cForGroup,
                        n[group], d[group], w[group], pForGroup);
            }
            objectiveFunction += bestCost;
        }

        System.out.println(objectiveFunction);

    }

    private static void selection(List<List<Integer>> solutions, int T, int[] delta, int r, int[] A, int[] B, int[] c, int m,
                                  int d, double w, int[] p) {
        int numberOfSolutions = solutions.size();
        Random random = new Random();
        for (int i = 0; i < numberOfSolutions; i++) {
            int pr = random.nextInt(100);
            if (pr < 20) {
                int index1 = random.nextInt(numberOfSolutions);
                int index2 = random.nextInt(numberOfSolutions);
                while (index1 == index2) {
                    index2 = random.nextInt(numberOfSolutions);
                }
                double cost1 = calculateFitnessFunction(T, delta, r, A, B, c, m, d, w, p, solutions.get(index1));
                double cost2 = calculateFitnessFunction(T, delta, r, A, B, c, m, d, w, p, solutions.get(index2));
                if (cost1 > cost2) {
                    Collections.copy(solutions.get(index2), solutions.get(index1));
                } else {
                    Collections.copy(solutions.get(index1), solutions.get(index2));
                }
            }
        }
    }

    private static List<Integer> crossover(List<Integer> parent) {
        Random random = new Random();
        int n = parent.size();
        int index1 = random.nextInt(n);
        int index2 = random.nextInt(n);
        int diff = Math.abs(index1 - index2);
        while (index1 == index2 || diff + 1 >= n) {
            index2 = random.nextInt(n);
            diff = Math.abs(index1 - index2);
        }
        if (index2 < index1) {
            int w = index1;
            index1 = index2;
            index2 = w;
        }
        List<Integer> child = new ArrayList<>(n);

        for (int i = index2 + 1; i < n; i++) {
            child.add(parent.get(i));
        }
        for (int i = 0; i < index1; i++) {
            child.add(parent.get(i));
        }
        for (int i = index1; i <= index2; i++) {
            child.add(parent.get(i));
        }

        return child;
    }

    private static double calculateFitnessFunction(int T, int[] delta, int r, int[] A, int[] B, int[] c, int m,
                                                    int d, double w, int[] p, List<Integer> solution) {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if (r + p[i] > B[j] || d - p[i] < A[j]) {
                    if (solution.get(i) == j) {
                        return 0;
                    }
                }
            }
        }
        int[] z = new int[T];
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r + p[k] <= B[i] &&
                    d - p[k] >= A[i]) {
                    int duration = 0;
                    for (int l = k; l < m; l++) {
                        if (solution.get(l) == i) {
                            duration += p[l];
                        }
                        int slotResidue = Math.min(B[i], d) -
                                          Math.max(A[i], r) - duration;
                        if (slotResidue < 0) {
                            if (-slotResidue < delta[i]) {
                                z[i] = Math.max(z[i], -slotResidue);
                            }
                            else {
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        int cost = 0;
        for (int i = 0; i < T; i++) {
            cost += c[i] * z[i];
        }
        if (cost <= C) {
            double value = 0;
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < T; j++) {
                    if (solution.get(i) == j) {
                        value += w;
                    }
                }
            }
            return value;
        } else {
            return 0;
        }
    }
}
