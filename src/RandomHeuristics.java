import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by acer on 23.06.2018.
 */
public class RandomHeuristics extends Heuristics {


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

        /*for (Map.Entry<Integer, ArrayList<Integer>> entry: N.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Integer v: entry.getValue())
                System.out.print(v + " ");
            System.out.println();
        }*/

        double maxObjValue = 0;
        int maxAssignedOperation = 0;
        Date date = new Date();
        for (int iteration = 0; iteration < 10; iteration++) {
            boolean[] isAssigned = new boolean[m];
            int[] durations = new int[T];
            int numberOfAssignedOperations = 0;

            for (int i = 0; i < T; i++) {
                ArrayList<Integer> operationsForCurrentSlot = N.get(i);
                Set<Integer> CL = operationsForCurrentSlot.stream()
                        .filter(op -> !isAssigned[op])
                        .collect(Collectors.toSet());

                final int start = A[i];
                final int finish = B[i];
                while (!CL.isEmpty()) {
                    //выбираем случайным образом операцию
                    int operationIndex = getRandomOperation(w, new ArrayList<>(CL), patientsAndGroups);
                    //ставим ее на интервал
                    durations[i] += p[operationIndex];
                    isAssigned[operationIndex] = true;
                    numberOfAssignedOperations++;
                    //System.out.println("Variable x[" + (operationIndex + 1) + "][" + (i + 1) + "]: Value = " + 1);
                    //проходим по всем операциям из CL и удаляем операции, которые уже не влезут в интервал
                    CL.remove(operationIndex);
                    final int dur = durations[i];
                    CL = CL.stream().filter(op -> dur + p[op] <= finish - start).collect(Collectors.toSet());
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
                        (int) (w[getGroupByIndex(o2, patientsAndGroups)] - w[getGroupByIndex(o1, patientsAndGroups)]));

                int totalCost = 0;
                for (int operation : operationsNumbers) {
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
                    } else
                        break;
                }
            }
            double objectiveFunctionValue = 0;
            for (int i = 0; i < m; i++) {
                if (isAssigned[i])
                    objectiveFunctionValue += w[getGroupByIndex(i, patientsAndGroups)];
            }

            if (objectiveFunctionValue > maxObjValue) {
                maxObjValue = objectiveFunctionValue;
                maxAssignedOperation = numberOfAssignedOperations;
            }

        }
        System.out.println("Количество назначенных пациентов: " + maxAssignedOperation);
        System.out.println("Optimal: " + maxObjValue);
        System.out.println("Время работы программы: " + (new Date().getTime() - date.getTime()) + " ms");

    }

    static int getRandomOperation(double[] w, List<Integer> CL, int[] patientsAndGroups) {
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
        return CL.get(i);
    }
}
