import java.util.*;

/**
 * Created by acer on 23.06.2018.
 */
public class NewHeuristics extends Heuristics {

    static ArrayList<Slot> setNewN(int[] A, int[] B, int[] r, int[] d, int m, int[] patientsAndGroups, int[] p) {
        int T = A.length;
        ArrayList<Slot> N = new ArrayList<>();
        for (int i = 0; i < T; i++) {
            if (i == 0 || A[i] != A[i - 1] || B[i] != B[i - 1]) {
                Slot currentSlot = new Slot(A[i], B[i]);
                currentSlot.timeSlotsIndexes.add(i);
                N.add(currentSlot);
                for (int j = 0; j < m; j++) {
                    if (d[getGroupByIndex(j, patientsAndGroups)] >= A[i] &&
                            B[i] >= r[getGroupByIndex(j, patientsAndGroups)]) {
                        currentSlot.operationsIndexes.add(j);
                    }
                }
                //сортирует по невозрастанию продолжительности операции
                Collections.sort(currentSlot.operationsIndexes, (o1, o2) -> p[o2] - p[o1]);
            }
            else {
                N.get(N.size() - 1).timeSlotsIndexes.add(i);
            }
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


        ArrayList<Slot> N = setNewN(A, B, r, d, m, patientsAndGroups, p);

        boolean[] isAssigned = new boolean[m];
        int[] durations = new int[T];
        int numberOfAssignedOperations = 0;

        Date date = new Date();
        for (Slot slot: N) {
            int currentTimeSlot = 0;
            for (int operation: slot.operationsIndexes) {
                if (!isAssigned[operation]) {
                    int iter = 0;
                    do {
                        int t = slot.timeSlotsIndexes.get(currentTimeSlot);
                        //если перебрали все интервалы, начинаем сначала
                        if (++currentTimeSlot == slot.timeSlotsIndexes.size())
                            currentTimeSlot = 0;
                        if (durations[t] + p[operation] <= B[t] - A[t]) {
                            durations[t] += p[operation];
                            isAssigned[operation] = true;
                            numberOfAssignedOperations++;
                            break;
                        }
                        else iter++;
                    } while (iter < slot.timeSlotsIndexes.size());
                }
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
        System.out.println("Время работы программы: " + (new Date().getTime() - date.getTime()) + " ms");
    }

    static class Slot {
        int A;
        int B;
        ArrayList<Integer> timeSlotsIndexes = new ArrayList<>();
        ArrayList<Integer> operationsIndexes = new ArrayList<>();

        public Slot(int A, int B) {
            this.A = A;
            this.B = B;
        }

    }
}
