package com.itranswarp.compiler;

public enum WeekDay {

    Mon("Monday"), Tue("Tuesday"), Wed("Wednesday"), Thu("Thursday"), Fri( "Friday"), Sat("Saturday");
    private final String day;
    private WeekDay(String day) {
        this.day = day;
    }
    public static void printDay(int i){
        switch(i){
            case 1: System.out.println(WeekDay.Mon); break;
            case 2: System.out.println(WeekDay.Tue);break;
            case 3: System.out.println(WeekDay.Wed);break;
            case 4: System.out.println(WeekDay.Thu);break;
            case 5: System.out.println(WeekDay.Fri);break;
            case 6: System.out.println(WeekDay.Sat);break;
            default:System.out.println("wrong number!");
        }
    }
    public String getDay() {
        return day;
    }
}
