package za.co.entelect.challenge.command;
//import za.co.entelect.challenge.entities.Worm;
//import java.util.List;

public class MoveCommand implements Command {

    private final int x;
    private final int y;
    private int selectedWorm;

    public int getX() {
        return x;
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

    public int getY() {
        return y;
    }

    public MoveCommand(int x, int y) {
        this.x = x;
        this.y = y;
        this.selectedWorm = -1;
    }

    public MoveCommand(int x, int y, int select) {
        this.x = x;
        this.y = y;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("move %d %d", x, y);
        }
        else {
            return String.format("select %d;move %d %d", selectedWorm, x, y);
        }
    }
}
