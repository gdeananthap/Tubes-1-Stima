package za.co.entelect.challenge.command;
//import za.co.entelect.challenge.entities.Worm;
//import java.util.List;

public class DigCommand implements Command {

    private final int x;
    private final int y;
    private int selectedWorm;

    public DigCommand(int x, int y) {
        this.x = x;
        this.y = y;
        this.selectedWorm = -1;
    }

//    public int countRangeFromWorm(List<Worm> enemyWorms){
//        int range = 0;
//        for (Worm enemyWorm : enemyWorms){
//            if (enemyWorm.health >0){
//                range += (int) (Math.sqrt(Math.pow(x - enemyWorm.position.x, 2) + Math.pow(y - enemyWorm.position.y, 2)));
//            }
//        }
//        return range;
//    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public DigCommand(int x, int y, int select) {
        this.x = x;
        this.y = y;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("dig %d %d", x, y);
        }
        else {
            return String.format("select %d;dig %d %d", selectedWorm, x, y);
        }
    }
}
