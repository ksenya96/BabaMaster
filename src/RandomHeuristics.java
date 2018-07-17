import java.util.*;

/**
 * Created by acer on 23.06.2018.
 */
public class RandomHeuristics extends Det {

    static int[] setSlotBounds(int T, boolean isBeginOfWorkDay) {
        int[] bounds = new int[T];
        int firstShiftTimeMoment = isBeginOfWorkDay ? FIRST_SHIFT_BEGIN : FIRST_SHIFT_END;
        int secondShiftTimeMoment = isBeginOfWorkDay ? SECOND_SHIFT_BEGIN : SECOND_SHIFT_END;
        int index = 0;
        for (int i = 0; i < weeks; i++) {
            for (int j = 0; j < days; j++) {
                for (int k = 0; k < rooms; k++) {
                    //первая смена
                    bounds[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + firstShiftTimeMoment * 60;
                }
                for (int k = 0; k < rooms; k++ ) {
                    //вторая смена
                    bounds[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + secondShiftTimeMoment * 60;
                }
            }
        }
        return bounds;
    }

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
            //сортирует по невозрастанию продолжтельности операции
            Collections.sort(N.get(i), new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if (p[o1] < p[o2])
                        return 1;
                    if (p[o1] > p[o2])
                        return -1;
                    return 0;
                }
            });
        }
        return N;
    }

    public static void main(String[] args) {
        int T = shifts * weeks * days * rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

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

        int[] p = setOperationsTimes(m);

        Map<Integer, ArrayList<Integer>> N = setN(A, B, r, d, m, patientsAndGroups, p);

        /*for (Map.Entry<Integer, ArrayList<Integer>> entry: N.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Integer v: entry.getValue())
                System.out.print(v + " ");
            System.out.println();
        }*/

        boolean[] isAssigned = new boolean[m];
        int[] durations = new int[T];
        int numberOfAssignedOperations = 0;

        for (int i = 0; i < T; i++) {
            ArrayList<Integer> operationsForCurrentSlot = N.get(i);
            //k - порядковый номер операции в массиве, но не действительный номер операции
            //действительный номер операции
            int operationIndex;
            for (int k = 0; k < operationsForCurrentSlot.size(); k++) {
                operationIndex = operationsForCurrentSlot.get(k);
                //формируем список операций, которые еще можно вместить в данный интервал
                ArrayList<Integer> CL = new ArrayList<>();
                double totalWeight = 0;
                if (!isAssigned[operationIndex] && durations[i] + p[operationIndex] <= B[i] - A[i]) {
                    CL.add(operationIndex);
                    totalWeight += w[getGroupByIndex(operationIndex, patientsAndGroups)];
                    /*durations[i] += p[operationIndex];
                    isAssigned[operationIndex] = true;
                    numberOfAssignedOperations++;
                    */
                }
                if (durations[i] == B[i] - A[i])
                    break;
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
            Collections.sort(operationsNumbers, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if (w[o1] < w[o2])
                        return 1;
                    if (w[o1] > w[o2])
                        return -1;
                    return 0;
                }
            });

            int totalCost = 0;
            for (int operation: operationsNumbers) {
                int minCost = 0;
                int index = -1;
                for (int i = 0; i < T; i++) {
                    //если операцию можно назначить на этот интервал
                    if (d[getGroupByIndex(operation, patientsAndGroups)] >= A[i] &&
                            B[i] >= r[getGroupByIndex(operation, patientsAndGroups)]) {
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
        System.out.println(objectiveFunctionValue);

    }
}
