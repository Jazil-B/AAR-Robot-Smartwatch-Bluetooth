package com.example.jazz.control;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CAP-ONE on 30/12/2016.
 */

public class ControlBDD {
    private ControlPrefs prefs;

    private int version;

    private SQLiteDatabase bdd;
    private BaseSQLite baseSQLite;
    private static final String NOM_BDD = "robot_control.db";
    private static final String TABLE = "robot_control";

    //ID
    private static final String COL_ID = "ID";
    private static final int NUM_COL_ID = 0;

    //Direction du robot
    private static final String COL_DIR = "Direction";
    private static final int NUM_COL_DIR= 1;

    // Durée de la direction
    private static final String COL_TIME = "Duree";
    private static final int NUM_COL_TIME = 3;

    public ControlBDD(Context context){
        prefs = new ControlPrefs(context);

        if((version = prefs.getIntPreference(ControlPrefs.DB_VERSION)) == 0) version = 1;

        //On créer la BDD et sa table
        baseSQLite = new BaseSQLite(context, NOM_BDD, null, version);

        prefs.saveIntPreference(ControlPrefs.DB_VERSION, version++);
    }

    public void open(){
        //on ouvre la BDD en écriture
        bdd = baseSQLite.getWritableDatabase();
    }

    public void close(){
        //on ferme l'accès à la BDD
        bdd.close();
    }

    public long insertDirection(String dir, int time){
        //Création d'un ContentValues (fonctionne comme une HashMap)
        ContentValues values = new ContentValues();
        //on lui ajoute une valeur associé à une clé (qui est le nom de la colonne dans laquelle on veut mettre la valeur)

        values.put(COL_DIR, dir);
        values.put(COL_TIME, time);

        //on insère l'objet dans la BDD via le ContentValues
        return bdd.insert(TABLE, null, values);
    }

    public List<Pair<String, Integer>> getDirections() {
        Cursor c = bdd.query(TABLE, new String[] {COL_ID, COL_DIR, COL_TIME}, null, null, null, null, null);
        return cursorToListDirections(c);
    }

    public List<Pair<String, Integer>> cursorToListDirections(Cursor c) {
        List<Pair<String, Integer>> list = new ArrayList<>();

        if (c.getCount() == 0)
            return null;

        //Sinon on se place sur le premier élément
        c.moveToFirst();

        while(!c.isLast()) {
            Pair<String, Integer> map = new Pair<>(c.getString(NUM_COL_DIR),c.getInt(NUM_COL_TIME));
            list.add(map);
            c.moveToNext();
        }

        //On ferme le cursor
        c.close();

        return list;
    }

    private class BaseSQLite extends SQLiteOpenHelper {

        private final String CREATE_BDD = "CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_DIR + " CHAR(1) NOT NULL, "
                + COL_TIME + " INTEGER NOT NULL);";

        public BaseSQLite(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //on créé la table à partir de la requête écrite dans la variable CREATE_BDD
            db.execSQL(CREATE_BDD);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //On peut fait ce qu'on veut ici moi j'ai décidé de supprimer la table et de la recréer
            //comme ça lorsque je change la version les id repartent de 0
            db.execSQL("DROP TABLE IF EXISTS " + TABLE + ";");
            onCreate(db);
        }
    }
}
