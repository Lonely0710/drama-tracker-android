package com.lonely.dramatracker.models;

import java.util.List;

/**
 * 每周放送时间表
 */
public class WeeklySchedule {
    private List<DailyAnime> sundayAnime;
    private List<DailyAnime> mondayAnime;
    private List<DailyAnime> tuesdayAnime;
    private List<DailyAnime> wednesdayAnime;
    private List<DailyAnime> thursdayAnime;
    private List<DailyAnime> fridayAnime;
    private List<DailyAnime> saturdayAnime;

    public List<DailyAnime> getSundayAnime() {
        return sundayAnime;
    }

    public void setSundayAnime(List<DailyAnime> sundayAnime) {
        this.sundayAnime = sundayAnime;
    }

    public List<DailyAnime> getMondayAnime() {
        return mondayAnime;
    }

    public void setMondayAnime(List<DailyAnime> mondayAnime) {
        this.mondayAnime = mondayAnime;
    }

    public List<DailyAnime> getTuesdayAnime() {
        return tuesdayAnime;
    }

    public void setTuesdayAnime(List<DailyAnime> tuesdayAnime) {
        this.tuesdayAnime = tuesdayAnime;
    }

    public List<DailyAnime> getWednesdayAnime() {
        return wednesdayAnime;
    }

    public void setWednesdayAnime(List<DailyAnime> wednesdayAnime) {
        this.wednesdayAnime = wednesdayAnime;
    }

    public List<DailyAnime> getThursdayAnime() {
        return thursdayAnime;
    }

    public void setThursdayAnime(List<DailyAnime> thursdayAnime) {
        this.thursdayAnime = thursdayAnime;
    }

    public List<DailyAnime> getFridayAnime() {
        return fridayAnime;
    }

    public void setFridayAnime(List<DailyAnime> fridayAnime) {
        this.fridayAnime = fridayAnime;
    }

    public List<DailyAnime> getSaturdayAnime() {
        return saturdayAnime;
    }

    public void setSaturdayAnime(List<DailyAnime> saturdayAnime) {
        this.saturdayAnime = saturdayAnime;
    }
} 