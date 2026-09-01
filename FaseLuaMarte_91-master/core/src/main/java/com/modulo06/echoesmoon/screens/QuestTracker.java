package com.modulo06.echoesmoon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class QuestTracker {

    private static QuestTracker instance;

    // Estados de conclusão das missões por planeta
    private boolean moonMissionComplete = false;
    private boolean marsMissionComplete = false;
    private boolean titanMissionComplete = false;

    // Marcos de progresso importantes
    private boolean weaponBuilt = false;
    private boolean titanPortalCreated = false;

    private QuestTracker() {
        loadProgress();
    }

    public static QuestTracker getInstance() {
        if (instance == null) {
            instance = new QuestTracker();
        }
        return instance;
    }

    public void loadProgress() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonQuests");
        moonMissionComplete = prefs.getBoolean("moonComplete", false);
        marsMissionComplete = prefs.getBoolean("marsComplete", false);
        titanMissionComplete = prefs.getBoolean("titanComplete", false);
        weaponBuilt = prefs.getBoolean("weaponBuilt", false);
        titanPortalCreated = prefs.getBoolean("titanPortalCreated", false);
    }

    public void saveProgress() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonQuests");
        prefs.putBoolean("moonComplete", moonMissionComplete);
        prefs.putBoolean("marsComplete", marsMissionComplete);
        prefs.putBoolean("titanComplete", titanMissionComplete);
        prefs.putBoolean("weaponBuilt", weaponBuilt);
        prefs.putBoolean("titanPortalCreated", titanPortalCreated);
        prefs.flush();
    }

    // Getters e Setters atualizados com salvamento automático
    public boolean isMoonMissionComplete() { return moonMissionComplete; }
    public void setMoonMissionComplete(boolean complete) {
        this.moonMissionComplete = complete;
        saveProgress();
    }

    public boolean isMarsMissionComplete() { return marsMissionComplete; }
    public void setMarsMissionComplete(boolean complete) {
        this.marsMissionComplete = complete;
        saveProgress();
    }

    public boolean isTitanMissionComplete() { return titanMissionComplete; }
    public void setTitanMissionComplete(boolean complete) {
        this.titanMissionComplete = complete;
        saveProgress();
    }

    public boolean isWeaponBuilt() { return weaponBuilt; }
    public void setWeaponBuilt(boolean built) {
        this.weaponBuilt = built;
        saveProgress();
    }

    public boolean isTitanPortalCreated() { return titanPortalCreated; }
    public void setTitanPortalCreated(boolean created) {
        this.titanPortalCreated = created;
        saveProgress();
    }
}
