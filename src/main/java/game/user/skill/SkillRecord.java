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
package game.user.skill;

/**
 *
 * @author Eric
 */
public class SkillRecord {
    private int skillID;
    private int info;
    
    public SkillRecord() {
        
    }
    
    public SkillRecord(int skillID, int info) {
        this.skillID = skillID;
        this.info = info;
    }
    
    public int getSkillID() {
        return skillID;
    }
    
    public int getInfo() {
        return info;
    }
    
    public void setSkillID(int skill) {
        this.skillID = skill;
    }
    
    public void setInfo(int info) {
        this.info = info;
    }
}
