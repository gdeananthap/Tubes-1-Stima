package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class Opponent {
    @SerializedName("id")
    public int id;

    @SerializedName("score")
    public int score;

    @SerializedName("worms")
    public Worm[] worms;

    // Tambahan Sendiri
    @SerializedName("currentWormId")
    public int opponentCurrentWormId;

    @SerializedName("previousCommand")
    public String opponentPreviousCommand;
}
