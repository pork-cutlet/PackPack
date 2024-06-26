package mandarin.packpack.supporter.event;

public class EventDate {
    public final int year;
    public final int month;
    public final int day;

    public final EventTimeSection section;

    public final boolean end;

    public EventDate(int value, boolean minus, EventTimeSection section, boolean end) {
        int[] temp = new int[3];

        temp[0] = value / 10000;

        value -= temp[0] * 10000;

        temp[1] = value / 100;

        value -= temp[1] * 100;

        temp[2] = value;

        if(minus)
            subtractOneDay(temp);

        year = temp[0];
        month = temp[1];
        day = temp[2];

        this.section = section;
        this.end = end;
    }

    public int compare(EventDate date) {
        int thisDate = getRawValue();
        int thatDate = date.getRawValue();

        return Integer.compare(thisDate, thatDate);
    }

    public int getRawValue() {
        int[] temp = {year, month, day};

        if(end && section.end.hour == 23 && section.end.minute == 59) {
            addOneDay(temp);
        }

        return temp[0] * 10000 + temp[1] * 100 + temp[2];
    }

    public boolean equals(EventDate date) {
        return compare(date) == 0;
    }

    public boolean notSame(EventDate date) {
        int thisDate = year * 10000 + month * 100 + day;
        int thatDate = date.year * 10000 + date.month * 100 + date.day;

        return thisDate != thatDate;
    }

    private void subtractOneDay(int[] temp) {
        if(temp[0] == 2030 && temp[1] == 1 && temp[2] == 1)
            return;

        temp[2]--;

        if(temp[2] == 0) {
            temp[1]--;

            if(temp[1] == 0) {
                temp[0]--;
                temp[1] = 12;
                temp[2] = 31;
            } else {
                temp[2] = getMaxMonthDay(temp[0], temp[1]);
            }
        }
    }

    private void addOneDay(int[] temp) {
        if(temp[0] == 2030 && temp[1] == 1 && temp[2] == 1)
            return;

        temp[2]++;

        if(temp[2] > getMaxMonthDay(temp[0], temp[1])) {
            temp[2] = 1;
            temp[1]++;

            if(temp[1] > 12) {
                temp[0]++;
                temp[1] = 1;
            }
        }
    }

    private int getMaxMonthDay(int year, int month) {
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            case 2:
                if(year % 4 == 0)
                    return 29;
                else
                    return 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                throw new IllegalStateException("Wrong month value : "+month);
        }
    }
}
