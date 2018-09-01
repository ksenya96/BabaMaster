import java.util.*;

/**
 * Created by acer on 23.06.2018.
 */
public class Heuristics extends Det {

    static void setSlotBounds(int[] A, int[] B, int T, int[] delta, int[] r) {
        int index = 0;
        for (int i = 0; i < weeks; i++) {
            for (int j = 0; j < days; j++) {
                for (int k = 0; k < rooms; k++) {
                    //первая смена
                    delta[index] = delta1;
                    A[index] =
                            7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_START_TIME * 60;
                    B[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_END_TIME * 60;
                }
                for (int k = 0; k < rooms; k++) {
                    //вторая смена
                    delta[index] = delta2;
                    A[index] =
                            7 * 24 * 60 * i + 24 * 60 * j + SECOND_SHIFT_START_TIME * 60;
                    B[index++] =
                            7 * 24 * 60 * i + 24 * 60 * j + SECOND_SHIFT_END_TIME * 60;
                }
                r[i * days + j] = 7 * 24 * 60 * i + 24 * 60 * j + FIRST_SHIFT_START_TIME * 60;
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

        /*for (Map.Entry<Integer, ArrayList<Integer>> entry: N.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Integer v: entry.getValue())
                System.out.print(v + " ");
            System.out.println();
        }*/

        boolean[] isAssigned = new boolean[m];
        int[] durations = new int[T];
        int numberOfAssignedOperations = 0;

        Date date = new Date();
        for (int i = 0; i < T; i++) {
            ArrayList<Integer> operationsForCurrentSlot = N.get(i);
            for (int operationIndex: operationsForCurrentSlot) {
                if (!isAssigned[operationIndex] && durations[i] + p[operationIndex] <= B[i] - A[i]) {
                    durations[i] += p[operationIndex];
                    isAssigned[operationIndex] = true;
                    numberOfAssignedOperations++;
                    //System.out.println("Variable x[" + (operationIndex + 1) + "][" + (i + 1) + "]: Value = " + 1);
                }
                if (durations[i] == B[i] - A[i])
                    break;
            }
            if (numberOfAssignedOperations == m)
                break;
        }

        if (numberOfAssignedOperations < m) {
            //сортируем оставшиеся операции в порядке невозрастания их весов
            ArrayList<Integer> operationsIndexes = new ArrayList<>();
            for (int i = 0; i < m; i++)
                if (!isAssigned[i])
                    operationsIndexes.add(i);

            Collections.sort(operationsIndexes, (o1, o2) ->
                    (int)(w[getGroupByIndex(o2, patientsAndGroups)] - w[getGroupByIndex(o1, patientsAndGroups)]));

            int totalCost = 0;
            for (int operation: operationsIndexes) {
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
        System.out.println("Optimal: " + objectiveFunctionValue);
        System.out.println("Время работы программы: " + (new Date().getTime() - date.getTime()) + " ms");
    }
}
