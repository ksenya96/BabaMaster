import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by acer on 23.06.2018.
 */
public class RandomHeuristics extends Det {

    static void setSlotBounds(int[] A, int[] B, int T, int[] delta, int[] r) {
        int index = 0;
        for (int i = 0; i < weeks; i++) {
            for (int j = 0; j < days; j++) {
                for (int k = 0; k < rooms; k++) {
                    //первая смена
                    delta[index] = delta1;
                    A[index] =
                            7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_BEGIN * 60;
                    B[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_END * 60;
                }
                for (int k = 0; k < rooms; k++) {
                    //вторая смена
                    delta[index] = delta2;
                    A[index] =
                            7 * 24 * 60 * i + 24 * 60 * j + SECOND_SHIFT_BEGIN * 60;
                    B[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + SECOND_SHIFT_END * 60;
                }
                r[i * days + j] = 7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_BEGIN * 60;
            }
        }
    }

    //здесь N - это словарь с ключами - номерами интервалов,
    // значениями - списком подходящих для данного интервала операций
    static Map<Integer, ArrayList<Integer>> setN(int[] A, int[] B, int[] r, int[] d, int m, int[] patientsAndGroups, int[] p) {
        int T = A.length;
        Map<Integer, ArrayList<Integer>> N = new HashMap<>();
        for (int i = 0; i < T; i++) {
            N.put(i, new ArrayList<>());
            for (int j = 0; j < m; j++) {
                if (d[getGroupByIndex(j, patientsAndGroups)] >= A[i] &&
                        B[i] >= r[getGroupByIndex(j, patientsAndGroups)]) {
                    N.get(i).add(j);
                }
            }
            //сортирует по невозрастанию продолжительности операции
            Collections.sort(N.get(i), (o1, o2) -> p[o2] - p[o1]);
        }
        return N;
    }

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

        Map<Integer, ArrayList<Integer>> N = setN(A, B, r, d, m, patientsAndGroups, p);

        for (Map.Entry<Integer, ArrayList<Integer>> entry: N.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Integer v: entry.getValue())
                System.out.print(v + " ");
            System.out.println();
        }

        boolean[] isAssigned = new boolean[m];
        int[] durations = new int[T];
        int numberOfAssignedOperations = 0;

        for (int i = 0; i < T; i++) {
            ArrayList<Integer> operationsForCurrentSlot = N.get(i);
            Set<Integer> CL = operationsForCurrentSlot.stream()
                    .filter(op -> !isAssigned[op])
                    .collect(Collectors.toSet());

            while (!CL.isEmpty()) {
                //выбираем случайным образом операцию
                int operationIndex = getRandomOperation(w, CL, patientsAndGroups);
                //ставим ее на интервал
                durations[i] += p[operationIndex];
                isAssigned[operationIndex] = true;
                numberOfAssignedOperations++;
                System.out.println("Variable x[" + (operationIndex + 1) + "][" + (i + 1) + "]: Value = " + 1);
                //проходим по всем операциям из CL и удаляем операции, которые уже не влезут в интервал
                CL.remove(operationIndex);
                final int dur = durations[i];
                final int start = A[i];
                final int finish = B[i];
                CL = CL.stream().filter(op -> dur + p[op] > finish - start).collect(Collectors.toSet());
            }

            if (numberOfAssignedOperations == m)
                break;
        }

        if (numberOfAssignedOperations < m) {
            //сортируем оставшиеся операции в порядке невозрастания их весов
            ArrayList<Integer> operationsNumbers = new ArrayList<>();
            for (int i = 0; i < m; i++)
                if (!isAssigned[i])
                    operationsNumbers.add(i);
            Collections.sort(operationsNumbers, (o1, o2) ->
                    (int)(w[getGroupByIndex(o2, patientsAndGroups)] - w[getGroupByIndex(o1, patientsAndGroups)]));

            int totalCost = 0;
            for (int operation: operationsNumbers) {
                int minCost = 0;
                int index = -1;
                for (int i = 0; i < T; i++) {
                    //если операцию можно назначить на этот интервал
                    if (d[getGroupByIndex(operation, patientsAndGroups)] >= A[i] &&
                            B[i] >= r[getGroupByIndex(operation, patientsAndGroups)] &&
                            durations[i] + p[operation] - B[i] + A[i] <= delta[i]) {
                        int cost = (durations[i] + p[operation] - B[i] + A[i]) * c[i];
                        if (minCost == 0 || cost < minCost) {
                            minCost = cost;
                            index = operation;
                        }
                    }
                }
                if (index != -1 && minCost + totalCost <= C) {
                    totalCost += minCost;
                    isAssigned[index] = true;
                    numberOfAssignedOperations++;
                }
                else
                    break;
            }
        }
        double objectiveFunctionValue = 0;
        for  (int i = 0; i < m; i++) {
            if (isAssigned[i])
                objectiveFunctionValue += w[getGroupByIndex(i, patientsAndGroups)];
        }
        System.out.println("Количество назначенных пациентов: " + numberOfAssignedOperations);
        System.out.println(objectiveFunctionValue);

    }

    static int getRandomOperation(double[] w, Set<Integer> CL, int[] patientsAndGroups) {
        double totalWeight = CL.stream().mapToDouble(op -> w[getGroupByIndex(op, patientsAndGroups)]).sum();
        List<Double> probabilities = CL.stream()
                .map(op -> w[getGroupByIndex(op, patientsAndGroups)] / totalWeight)
                .collect(Collectors.toList());
        double r = Math.random();
        int i = 0;
        double s = probabilities.get(0);
        while (i < CL.size() && !(r < s)) {
            s += probabilities.get(++i);
        }
        return 0;
    }
}
