import java.util.*;

public class LocalSearch extends Heuristics {
    public static void main(String[] args) {
        int T = shifts * weeks * days * rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

        int[] delta = new int[T];
        int G = weeks * days; //кол-во групп
        int[] r = new int[G];
        int[] A = new int[T];
        int[] B = new int[T];
        setSlotBounds(A, B, T, delta, r);

        int[] c = setCostPerUnitTimeOfExtension(T); //стоимость единицы времени увеличения

        int[] n = setNumberOfPatientsInGroups(G); //кол-во пациентов в группах
        int m = getTotalNumberOfPatients(n); //кол-во операций
        int[] patientsAndGroups = getGroupsForPatients(m, n);
        System.out.println("Кол-во операций m = " + m);

        int[] d = setDueDates(r); //конечные сроки

        double[] w = setOperationsWeights(n); //веса групп операций

        int[] p = setOperationsTimes(m);

        //начальная популяция
        int numberOfSolutions = 100;
        Random random = new Random();
        List<List<Integer>> solutions = new ArrayList<>(numberOfSolutions);
        for (int i = 0; i < numberOfSolutions; i++) {
            solutions.add(new ArrayList<>(m));
        }

        /*Map<Integer, ArrayList<Integer>> passedSlots = new HashMap<>();
        for (int i = 0; i < m; i++) {
            passedSlots.put(i, new ArrayList<>());
        }
        //формирование списка допустимых интервалов для каждой операции
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                    d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    passedSlots.get(k).add(i);
                }
            }
        }*/

        //генерация случайных решений
        for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            for (int i = 0; i < m; i++) {
                int slot = random.nextInt(T);
                solutions.get(cnt).add(slot);
            }
        }

        //вычисление функции приспособленности
        double[] fitnessFunctionValues = new double[numberOfSolutions + 1];
        for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            fitnessFunctionValues[cnt] =
                    calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, solutions.get(cnt));
        }

        /*for (int i = 0; i < numberOfSolutions; i++) {
            int patients = 0;
            if (fitnessFunctionValues[i] > 0.0) {
                for (int j = 0; j < m; j++) {
                    for (int k = 0; k < T; k++) {
                        patients += x.get(i).get(j).get(k);
                    }
                }
            }
            System.out.println(fitnessFunctionValues[i] + " " + patients);
        }*/

        double bestCost = -1;
        int bestIndex = -1;
        List<Integer> bestSolution;

        //evaluate
        for (int i = 0; i < solutions.size(); i++) {
            double cost = calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, solutions.get(i));
            if (cost > bestCost) {
                bestCost = cost;
                bestIndex = i;
                System.out.println("Best cost " + bestCost + " iteration " + 1 + solutions.get(bestIndex));
            }
        }

        for (int cnt = 0; cnt < 100; cnt++) {
            bestSolution = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                bestSolution.add(0);
            }
            Collections.copy(bestSolution, solutions.get(bestIndex));
            //выбираем родителей
            for (List<Integer> solution: solutions) {
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
            int index = random.nextInt(numberOfSolutions);
            while (index == bestIndex) {
                index = random.nextInt(numberOfSolutions);
            }
            Collections.copy(solutions.get(index), bestSolution);

            //evaluate
            for (int i = 0; i < solutions.size(); i++) {
                double cost = calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, solutions.get(i));
                if (cost > bestCost) {
                    bestCost = cost;
                    bestIndex = i;
                    System.out.println("Best cost " + bestCost + " iteration " + cnt + solutions.get(bestIndex));
                }
            }
            //отбор
            selection(solutions, T, delta, r, A, B, c, m, patientsAndGroups, d, w, p);
        }

    }

    private static void selection(List<List<Integer>> solutions, int T, int[] delta, int[] r, int[] A, int[] B, int[] c, int m,
                                  int[] patientsAndGroups, int[] d, double[] w, int[] p) {
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
                double cost1 = calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, solutions.get(index1));
                double cost2 = calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, solutions.get(index2));
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

    private static double calculateFitnessFunction(int T, int[] delta, int[] r, int[] A, int[] B, int[] c, int m,
                                                   int[] patientsAndGroups, int[] d, double[] w, int[] p, List<Integer> solution) {
        int[] z = new int[T];
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                    d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    int duration = 0;
                    for (int l = k; l < m; l++) {
                        if (solution.get(l) == i) {
                            duration += p[l];
                        }
                        int slotResidue = Math.min(B[i], d[getGroupByIndex(l, patientsAndGroups)]) -
                                          Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]) - duration;
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
                        value += w[getGroupByIndex(i, patientsAndGroups)];
                    }
                }
            }
            return value;
        } else {
            return 0;
        }
    }
}
