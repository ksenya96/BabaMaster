import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

/**
 * Created by acer on 03.08.2018.
 */
public class Uncertain extends ExtDetAlt {

    static int[][] setOperationsTimes(int s, int m) {
        int[][] p = new int[s][m];
        for (int i = 0; i < m; i++) {
            p[0][i] = FIRST_SHIFT_END_TIME - FIRST_SHIFT_START_TIME;
            p[1][i] = FIRST_SHIFT_END_TIME - FIRST_SHIFT_START_TIME / 2;
            p[2][i] = (FIRST_SHIFT_END_TIME - FIRST_SHIFT_START_TIME) / 3;
            p[3][i] = 20;
        }
        return p;
    }

    public static void main(String[] args) {
        int s = 4; //кол-во сценариев

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

        int[][] p = setOperationsTimes(s, m);


        try {
            IloCplex cplex = new IloCplex();
            IloIntVar[] x = setX(T, m, cplex);
            IloNumVar[] y = setY(m, cplex);
            IloNumVar[] z = setZ(T, cplex);


            //целевая функция
            setObjectiveFunction(patientsAndGroups, w, cplex, x, y, A);

            ArrayList<IloRange> ranges = new ArrayList<>();
            //ограничения
            //суммарная стоимость увеличения не должна превысить порог C
            setCostConstraint(c, z, cplex, ranges);
            System.out.println("Constr1 is set");

            //сумма по x[j][t] - y[j] = 0 по всем j
            setConstraintsForXAndY(cplex, x, y, ranges);
            System.out.println("Constr2 are set");

            //x[j][t] = 0, если t-й времменной интервал недопустим для операции j
            setXEqZeroConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, ranges);
            System.out.println("Constr3 are set");

            setConstraintsForX(A, B, cplex, x, ranges);
            System.out.println("Constr4 are set");

            /*int prev = 0;
            for (int i = 1; i < T; i++) {
                if (A[prev] == A[i] && B[prev] == B[i]) {
                    for (int j = 0; j < m; j++) {
                        if (r[getGroupByIndex(j, patientsAndGroups)] + p[j] <= B[i] &&
                                d[getGroupByIndex(j, patientsAndGroups)] - p[j] >= A[i]) {
                            ranges.add(cplex.addGe(cplex.diff(x[j * T + prev], x[j * T + i]), 0));
                        }
                    }
                }
                prev = i;
            }
            System.out.println("Constr4 are set");*/

            setMainConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, z, ranges);
            System.out.println("Constr5 are set");


            if (cplex.solve()) {
                cplex.output().println(cplex.getStatus());
                cplex.output().println(cplex.getObjValue());
                double[] resX     = cplex.getValues(x);
                int cnt = 0;
                for (int i = 0; i < m; i++) {
                    for (int j = 0; j < T; j++) {
                        if (resX[i * T + j] == 1)
                            cnt++;
                        //System.out.print((int)resX[i * T + j]);
                        //System.out.println("Variable x[" + (i + 1) + "][" + (j + 1) + "]: Value = " + resX[i * T + j]);
                    }
                    //System.out.println();
                }
                System.out.println("Кол-во назначенных операций " + cnt);

                /*double[] resZ = cplex.getValues(z);
                for (int i = 0; i < T; i++) {
                    System.out.println("z[" + i + "] = " + resZ[i]);
                }*/

                /*for (int j = 0; j < T; ++j) {
                    double duration = A[j];
                    for (int i = 0; i < m; i++) {
                        duration += p[i] * resX[i * T + j];
                    }
                    double res = duration > B[j] ? duration - B[j] : 0;
                    System.out.println("z[" + j + "] = " + res);
                }*/

                /*double[] slacks = cplex.getSlacks(ranges.toArray(new IloRange[ranges.size()]));
                for (int i = 0; i < slacks.length; i++)
                    System.out.println(slacks[i]);
                */

            }
            cplex.exportModel("mipex1.lp");
            cplex.end();
        }
        catch (IloException e) {
            System.err.println("Concert exception caught " + e);
        }
    }

    static void setMainConstraints(int[] A, int[] B, int[] patientsAndGroups, int[] r, int[] d, int[][] p, IloCplex cplex, IloIntVar[] x, IloNumVar[] z, ArrayList<IloRange> ranges) throws IloException {
        int T = A.length;
        int m = p.length;

        for (int i = 0; i < p.length; i++) {
            for (int j = 0; j < T; j++) {
                for (int k = 0; k < m; k++) {
                    if (r[getGroupByIndex(k, patientsAndGroups)] + max(p, k) <= B[j] &&
                            d[getGroupByIndex(k, patientsAndGroups)] - max(p, k) >= A[j]) {
                        IloLinearIntExpr duration = cplex.linearIntExpr();
                        for (int l = k; l < m; l++) {
                            duration.addTerm(p[i][l], x[l * T + j]);

                            //if (B[i] != d[getGroupByIndex(l, patientsAndGroups)])
                            ranges.add(cplex.addLe(
                                    cplex.diff(
                                            cplex.sum(Math.max(A[j], r[getGroupByIndex(k, patientsAndGroups)]), duration),
                                            cplex.sum(Math.min(B[j], d[getGroupByIndex(l, patientsAndGroups)]),
                                                    z[B[j] <= d[getGroupByIndex(l, patientsAndGroups)] ? j :
                                                            weeks * days * rooms + rooms * getGroupByIndex(l, patientsAndGroups)])
                                    ),
                                    0));
                        /*else
                            ranges.add(cplex.addEq(z[j], 0));*/

                        /*ranges.add(cplex.addLe(
                                cplex.sum(Math.max(A[j], r[getGroupByIndex(k, patientsAndGroups)]), duration),
                                d[getGroupByIndex(l, patientsAndGroups)]));*/
                        }
                    }
                }
            }
        }
    }

    static void setXEqZeroConstraints(int[] A, int[] B, int[] patientsAndGroups, int[] r, int[] d, int[][] p, IloCplex cplex, IloIntVar[] x, ArrayList<IloRange> ranges) throws IloException {
        int m = p.length;
        int T = A.length;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if (r[getGroupByIndex(i, patientsAndGroups)] + max(p, i) > B[j] || d[getGroupByIndex(i, patientsAndGroups)] - max(p, i) < A[j]) {
                    ranges.add(cplex.addEq(x[i * T + j], 0));
                }
            }
        }
    }

    private static int max(int[][] p, int col) {
        int max = p[0][col];
        for (int i = 1; i < p.length; i++)
            if (p[i][col] > max)
                max = p[i][col];
        return max;
    }
}
