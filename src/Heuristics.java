import java.util.Arrays;

/**
 * Created by acer on 23.06.2018.
 */
public class Heuristics {
    static class TimeSlot implements Comparable<TimeSlot> {
        int A;
        int B;

        @Override
        public int compareTo(TimeSlot o) {
            if (this.A > o.A)
                return 1;
            if (this.A < o.A)
                return -1;
            return 0;
        }
    }

    static TimeSlot[] setSlots(int T) {
        TimeSlot[] slots = new TimeSlot[T];
        for (int i = 0; i < T; i++)
            slots[i] = new TimeSlot();
        for (int i = 0; i < Det.weeks; i++) {
            for (int j = 0; j < Det.days; j++) {
                for (int k = 0; k < Det.rooms; k++) {
                    //первая смена
                    slots[Det.days * Det.rooms * i + Det.rooms * j + k].A =
                            7 * 24 * 60 * i + 24 * 60 * j + Det.FIRST_SHIFT_BEGIN * 60;
                    slots[Det.days * Det.rooms * i + Det.rooms * j + k].B =
                            7 * 24 * 60 * i + 24 * 60 * j + Det.FIRST_SHIFT_END * 60;
                    //вторая смена
                    slots[Det.weeks * Det.days * Det.rooms + Det.days * Det.rooms * i + Det.rooms * j + k].A =
                            7 * 24 * 60 * i + 24 * 60 * j + Det.SECOND_SHIFT_BEGIN * 60;
                    slots[Det.weeks * Det.days * Det.rooms + Det.days * Det.rooms * i + Det.rooms * j + k].B =
                            7 * 24 * 60 * i + 24 * 60 * j + Det.SECOND_SHIFT_END * 60;
                }
            }
        }
        return slots;
    }

    public static void main(String[] args) {
        int T = Det.shifts * Det.weeks * Det.days * Det.rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

        TimeSlot[] slots = setSlots(T);
        Arrays.sort(slots);
    }
}
