import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        byte[][][] x = new byte[numberOfSolutions][m][T];
        Map<Integer, ArrayList<Integer>> passedSlots = new HashMap<>();
        for (int i = 0; i < m; i++) {
            passedSlots.put(i, new ArrayList<>());
        }
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                    d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    passedSlots.get(k).add(i);
                }
            }
        }

        for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            for (int i = 0; i < m; i++) {
                int slot = random.nextInt(passedSlots.get(i).size());
                x[cnt][i][slot] = (byte)random.nextInt(2);
            }
        }

        double[] fitnessFunctionValues = new double[numberOfSolutions];
        A: for (int cnt = 0; cnt < numberOfSolutions; cnt++) {
            int[] z = new int[T];
            for (int i = 0; i < T; i++) {
                for (int k = 0; k < m; k++) {
                    if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                        d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                        int duration = 0;
                        for (int l = k; l < m; l++) {
                            duration += p[l] * x[cnt][l][i];
                            int slotResidue = Math.min(B[i], d[getGroupByIndex(l, patientsAndGroups)]) -
                                              Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]) - duration;
                            if (slotResidue < 0) {
                                if (-slotResidue < delta[i]) {
                                    z[i] = Math.max(z[i], -slotResidue);
                                }
                                else {
                                    fitnessFunctionValues[cnt] = 0;
                                    continue A;
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
                        value += w[getGroupByIndex(i, patientsAndGroups)] * x[cnt][i][j];
                    }
                }
                fitnessFunctionValues[cnt] = value;
            } else {
                fitnessFunctionValues[cnt] = 0;
            }
        }

        for (int i = 0; i < numberOfSolutions; i++) {
            int patients = 0;
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < T; k++) {
                    patients += x[i][j][k];
                }
            }
            System.out.println(fitnessFunctionValues[i] + " " + patients);
        }

    }
}
