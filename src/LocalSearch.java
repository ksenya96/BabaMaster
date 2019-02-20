import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

        int numberOfSolutions = 10;
        Random random = new Random();
        List<List<List<Byte>>> x = new ArrayList<>(numberOfSolutions);
        for (int i = 0; i < numberOfSolutions; i++) {
            x.add(new ArrayList<>(m));
            for (int j = 0; j < m; j++) {
                x.get(i).add(new ArrayList<>(T));
                for (int k = 0; k < T; k++) {
                    x.get(i).get(j).add((byte)0);
                }
            }
        }

        Map<Integer, ArrayList<Integer>> passedSlots = new HashMap<>();
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
        }

        //генерация случайных решений
        for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            for (int i = 0; i < m; i++) {
                int slot = random.nextInt(T);
                x.get(cnt).get(i).set(slot, (byte)random.nextInt(2));
            }
        }

        //вычисление функции приспособленности
        double[] fitnessFunctionValues = new double[numberOfSolutions + 1];
        for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            fitnessFunctionValues[cnt] =
                    calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, x.get(cnt));
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

        for (int iter = 0; iter < 100; iter++) {
            //вычисление вероятностей выбора решений из популяции
            double sumFitnessFunctionValues = Arrays.stream(fitnessFunctionValues).sum() - fitnessFunctionValues[numberOfSolutions];
            double[] pr = Arrays.stream(fitnessFunctionValues).map((v) -> v / sumFitnessFunctionValues).limit(numberOfSolutions).toArray();
            int parentSolution1 = getRandomValue(pr);
            int parentSolution2 = getRandomValue(pr);
            System.out.println(parentSolution1 + " " + parentSolution2);

            //скрещивание
            List<List<Byte>> newSolution = crossover(T, m, x, parentSolution1, parentSolution2);

            //мутация
            double randomParameter = 0.3;
            mutation(T, m, newSolution, randomParameter);
            x.add(newSolution);
            fitnessFunctionValues[numberOfSolutions] = calculateFitnessFunction(T, delta, r, A, B, c, m, patientsAndGroups, d, w, p, newSolution);
            System.out.println(fitnessFunctionValues[numberOfSolutions]);

            int minIndex = 0;
            for (int i = 1; i < numberOfSolutions + 1; i++) {
                if (fitnessFunctionValues[i] < fitnessFunctionValues[minIndex]) {
                    minIndex = i;
                }
            }
            x.remove(minIndex);
            fitnessFunctionValues[minIndex] = fitnessFunctionValues[numberOfSolutions];
            System.out.println(Arrays.stream(fitnessFunctionValues).max().getAsDouble());
        }

    }

    private static List<List<Byte>> crossover(int T, int m, List<List<List<Byte>>> x, int parentSolution1, int parentSolution2) {
        List<List<Byte>> newSolution = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            newSolution.add(new ArrayList<>(T));
            for (int j = 0; j < T; j++) {
                newSolution.get(i).add((byte)0);
            }
        }
        Random random = new Random();
        for (int i = 0; i < m; i++) {
            int parent1Flag = -1;
            int parent2Flag = -1;
            for (int j = 0; j < T; j++){
                if (x.get(parentSolution1).get(i).get(j) == 1) {
                    parent1Flag = j;
                }
                if (x.get(parentSolution2).get(i).get(j) == 1) {
                    parent2Flag = j;
                }
            }
            if (random.nextInt(2) == 0) {
                if (parent1Flag > -1) {
                    newSolution.get(i).set(parent1Flag, (byte)1);
                }
            } else {
                if (parent2Flag > -1) {
                    newSolution.get(i).set(parent2Flag, (byte)1);
                }
            }
        }
        return newSolution;
    }

    private static void mutation(int T, int m, List<List<Byte>> newSolution, double randomParameter) {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if (newSolution.get(i).get(j) == 1 && getRandomValue(randomParameter, 1 - randomParameter) == 0) {
                    //инверсия
                    newSolution.get(i).set(j, (byte)0); //(byte)Math.abs(newSolution[i][j] - 1);
                }
            }
        }
    }

    private static double calculateFitnessFunction(int T, int[] delta, int[] r, int[] A, int[] B, int[] c, int m,
                                                   int[] patientsAndGroups, int[] d, double[] w, int[] p, List<List<Byte>> x) {
        int[] z = new int[T];
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                    d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    int duration = 0;
                    for (int l = k; l < m; l++) {
                        duration += p[l] * x.get(l).get(i);
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
                    value += w[getGroupByIndex(i, patientsAndGroups)] * x.get(i).get(j);
                }
            }
            return value;
        } else {
            return 0;
        }
    }

    static int getRandomValue(double... pr) {
        double r = Math.random();
        int i = 0;
        double s = pr[0];
        while (i < pr.length && !(r < s)) {
            s += pr[++i];
        }
        return i;
    }
}
