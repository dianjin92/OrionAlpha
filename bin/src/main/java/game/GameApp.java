/*
 * This file is part of OrionAlpha, a MapleStory Emulator Project.
 * Copyright (C) 2018 Eric Smith <notericsoft@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package game;

import game.field.FieldMan;
import game.field.life.mob.MobTemplate;
import game.field.life.npc.NpcTemplate;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import network.GameAcceptor;
import network.database.Database;
import network.database.GameDB;
import util.Logger;
import util.TimerThread;

/**
 *
 * @author Eric
 */
public class GameApp implements Runnable {
    private static final GameApp instance = new GameApp();
    
    private GameAcceptor acceptor;
    private CenterSocket socket;
    private int connectionLimit;
    private int waitingFirstPacket;
    private byte worldID;
    private final long serverStartTime;
    private final AtomicLong itemInitSN;
    private final AtomicLong cashItemInitSN;
    private final Lock lockItemSN;
    private final Lock lockCashItemSN;
    
    public GameApp() {
        this.connectionLimit = 4000;
        this.waitingFirstPacket = 1000 * 15;
        this.serverStartTime = System.currentTimeMillis();
        this.itemInitSN = new AtomicLong(0);
        this.cashItemInitSN = new AtomicLong(0);
        this.lockItemSN = new ReentrantLock();
        this.lockCashItemSN = new ReentrantLock();
    }
    
    public static GameApp getInstance() {
        return instance;
    }
    
    private void connectCenter() {
        this.socket = new CenterSocket();
        this.socket.connect();
        
        this.worldID = Byte.parseByte(System.getProperty("gameID", "0"));
    }
    
    private void createAcceptor() {
        try (JsonReader reader = Json.createReader(new FileReader(String.format("Game%d.img", getWorldID())))) {
            JsonObject gameData = reader.readObject();
            
            String ip = gameData.getString("PublicIP", "127.0.0.1");
            int port = gameData.getInt("port", 8585);
            
            acceptor = new GameAcceptor(new InetSocketAddress(ip, port));
            acceptor.run();
            
            Logger.logReport("Socket acceptor started");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
    }
    
    public final GameAcceptor getAcceptor() {
        return acceptor;
    }
    
    public final long getNextCashSN() {
        lockCashItemSN.lock();
        try {
            final long cashItemSN = cashItemInitSN.incrementAndGet();
            
            return cashItemSN;
        } finally {
            lockCashItemSN.unlock();
        }
    }
    
    public final long getNextSN() {
        lockItemSN.lock();
        try {
            final long itemSN = itemInitSN.incrementAndGet();
            
            return itemSN;
        } finally {
            lockItemSN.unlock();
        }
    }
    
    public int getConnectionLimit() {
        return connectionLimit;
    }
    
    public long getServerStartTime() {
        return serverStartTime;
    }
    
    public int getWaitingFirstPacket() {
        return waitingFirstPacket;
    }
    
    public byte getWorldID() {
        return worldID;
    }
    
    private void initializeDB() {
        try (JsonReader reader = Json.createReader(new FileReader("Database.img"))) {
            JsonObject dbData = reader.readObject();
            
            int dbPort = dbData.getInt("dbPort", 3306);
            String dbName = dbData.getString("dbGameWorld", "orionalpha");
            String dbSource = dbData.getString("dbGameWorldSource", "127.0.0.1");
            String[] dbInfo = dbData.getString("dbGameWorldInfo", "root,").split(",");
            
            // Construct the instance of the Database
            Database.createInstance(dbName, dbSource, dbInfo[0], dbInfo.length == 1 ? "" : dbInfo[1], dbPort);
            
            // Load the initial instance of the Database
            Database.getDB().load();
            
            Logger.logReport("DB configuration parsed successfully");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
    }
    
    private void initializeGameData() {
        long time;
        
        // Load Items and Equipment
        
        // Load Skills
        
        // Load Mobs
        time = System.currentTimeMillis();
        MobTemplate.load();
        Logger.logReport("Loaded Mob Attributes in " + ((System.currentTimeMillis() - time) / 1000.0) + " seconds.");
        
        // Load Npcs
        time = System.currentTimeMillis();
        NpcTemplate.load();
        Logger.logReport("Loaded Npc Attributes in " + ((System.currentTimeMillis() - time) / 1000.0) + " seconds.");
        
        // Load Maps
        time = System.currentTimeMillis();
        FieldMan.getInstance();
        Logger.logReport("Loaded map (field) data from map files in " + ((System.currentTimeMillis() - time) / 1000.0) + " seconds.");
    }
    
    private void initializeItemSN() {
        GameDB.rawLoadItemInitSN(this.worldID, this.itemInitSN, this.cashItemInitSN);
    }
    
    public static void main(String[] args) {
        GameApp.getInstance().run();
    }
    
    @Override
    public void run() {
        TimerThread.createTimerThread();
        
        initializeDB();
        initializeItemSN();
        initializeGameData();
        connectCenter();
        createAcceptor();
        Logger.logReport("The Game Server has been initialized in " + ((System.currentTimeMillis() - serverStartTime) / 1000.0) + " seconds.");
    }
    
    public void setConnectionLimit(int limit) {
        this.connectionLimit = limit;
    }
    
    public void updateItemInitSN() {
        GameDB.rawUpdateItemInitSN(this.worldID, this.itemInitSN, this.cashItemInitSN);
    }
}
