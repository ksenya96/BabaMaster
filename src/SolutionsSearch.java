/**
 * Created by acer on 08.06.2018.
 */
public class SolutionsSearch extends Det {
    public static void main(String[] args) {
        int T = shifts * weeks * days * rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

        //в качестве единиц измерения берем минуты
        int[] A = setSlotBounds(T, true); //начальные моменты интервалов
        int[] B = setSlotBounds(T, false); //конечные моменты интервалов

        int[] c = setCostPerUnitTimeOfExtension(T); //стоимость единицы времени увеличения
        int G = days; //кол-во групп

        int[] n = setNumberOfPatientsInGroups(G); //кол-во пациентов в группах
        int m = getTotalNumberOfPatients(n); //кол-во операций
        int[] patientsAndGroups = getGroupsForPatients(m, n);
        System.out.println("Кол-во операций m = " + m);

        int[] r = setReadyDates(G, A); //сроки готовности для группы операций
        int[] d = setDueDates(r); //конечные сроки

        double[] w = setOperationsWeights(n); //веса групп операций

        int[] p = setOperationsTimes(m);

        String x;
        long bitmask = 0;
        int optimal = 0;
        int[] z = new int[T];
        String resX = "";
        while (bitmask < Math.pow(2, m * T)) {
            x = Long.toBinaryString(bitmask);
            bitmask += 1;
            if (!checkSumX(x, m, T))
                continue;
            if (!checkXEqZero(x, m, T, A, B, r, d, p, patientsAndGroups))
                continue;
            if (checkOtherConditions(x, m, T, A, B, r, d, p, patientsAndGroups, c, z)) {
                int res = getWeight(x, w, m, T, patientsAndGroups);
                if (res >= optimal) {
                    optimal = res;
                    System.out.println(optimal);
                    resX = new String(x);
                }
            }
        }
        System.out.println(resX);
    }

    private static boolean checkOtherConditions(String x, int m, int T,
                                                int[] A, int[] B, int[] r, int[] d,
                                                int[] p, int[] patientsAndGroups, int[] c,
                                                int[] z) {
        int cost = 0;
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                        d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    for (int l = k; l < m; l++) {
                        int duration = 0;
                        for (int j = k; j <= l; j++)
                            if (j * T + i >= x.length() || x.charAt(j * T + i) == '1')
                                duration += p[j];
                        if (Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]) + duration > d[getGroupByIndex(l, patientsAndGroups)])
                            return false;
                        z[i] = Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]) + duration - B[i];
                        if (i < T / 2) {
                            if (z[i] > delta1)
                                return false;
                        } else if (z[i] > delta2)
                            return false;
                    }
                }
            }
        }

        for (int i = 0; i < T; i++)
            cost += z[i] * c[i];
        if (cost > C)
            return false;
        return true;
    }

    private static int getWeight(String x, double[] w, int m, int T, int[] patientsAndGroups) {
        int sum = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if (i * T + j >= x.length() || x.charAt(i * T + j) == '1')
                    sum += w[getGroupByIndex(i, patientsAndGroups)];
            }
        }
        return sum;
    }

    private static boolean checkSumX(String x, int m, int T) {
        int sum;
        for (int i = 0; i < m; i++) {
            sum = 0;
            for (int j = 0; j < T; j++) {
                if (i * T + j >= x.length() || x.charAt(i * T + j) == '1')
                    sum++;
            }
            if (sum > 1)
                return false;
        }
        return true;
    }

    private static boolean checkXEqZero(String x, int m, int T,
                                        int[] A, int[] B,
                                        int[] r, int[] d,
                                        int[] p, int[] patientsAndGroups) {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if ((r[getGroupByIndex(i, patientsAndGroups)] + p[i] > B[j] || d[getGroupByIndex(i, patientsAndGroups)] - p[i] < A[j])
                        && (i * T + j >= x.length() || x.charAt(i * T + j) == '1')) {
                    return false;
                }
            }
        }
        return true;
    }

}
