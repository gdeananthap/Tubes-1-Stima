package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class Worm {
    @SerializedName("id")
    public int id;

    @SerializedName("health")
    public int health;

    public int getHealth(){
        return health;
    }

    @SerializedName("position")
    public Position position;

    @SerializedName("diggingRange")
    public int diggingRange;

    @SerializedName("movementRange")
    public int movementRange;

    @SerializedName("roundsUntilUnfrozen")
    public int frozenTime;

    @SerializedName("profession")
    public String profession;
}
