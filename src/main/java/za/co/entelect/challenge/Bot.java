package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private  MyPlayer myPlayer;
    private MyWorm currentWorm;


    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.myPlayer = gameState.myPlayer;
        this.currentWorm = getCurrentWorm(gameState);

    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }
    MyWorm findClosestFriendWorm(GameState gameState) {
        int distance = 1000000000;
        MyWorm selectedWorm = null;
        for(MyWorm myWorm : myPlayer.worms){
            if (myWorm.health >0 && myWorm !=currentWorm){
                if (euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y) < distance){
                    distance = euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y);
                    selectedWorm =myWorm;
                }
            }
        }
        return selectedWorm;
    }

    Command GetToCentre(){
        //  Mencoba gerak ke tengah
        //  Center Class Declaration
        Position Center = new Position();
        Center.x = 16;
        Center.y = 16;
        // Greedy by Lava Location
        int currRound = gameState.currentRound;
        int currWormDistanceToCenter = euclideanDistance(currentWorm.position.x,currentWorm.position.y,Center.x, Center.y);
        // Apakah layak mendekat ke tengah
        if ((currRound >= 70 && currWormDistanceToCenter >= 14)||(currRound >= 140 && currWormDistanceToCenter >= 10)||(currRound >= 210 && currWormDistanceToCenter >= 7)||(currRound >= 240 && currWormDistanceToCenter >= 5)||(currRound >= 320 && currWormDistanceToCenter >= 3)) {
            Direction toCentre = resolveDirection(currentWorm.position, Center);
            Cell SelectedBlock = gameState.map[currentWorm.position.y + toCentre.y][currentWorm.position.x + toCentre.x];
            if (SelectedBlock.type == CellType.AIR) {
                return new MoveCommand(SelectedBlock.x, SelectedBlock.y);
            } else if (SelectedBlock.type == CellType.DIRT) {
                return new DigCommand(SelectedBlock.x, SelectedBlock.y);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    boolean wormAlone(Worm inDanger){
        boolean alone = true;
        for (Worm myWorm : myPlayer.worms){
            if (myWorm != inDanger && myWorm.health>0 && euclideanDistance(myWorm.position.x,myWorm.position.y,inDanger.position.x,inDanger.position.y) >= 4){
                alone = false;
            }
        }
        return alone;
    }
    Worm PredictTargetedWorm(){
        List<Integer> noWormTerdekat = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms){
            List<Integer> jarak2 = new ArrayList<>();
            for (Worm myWorm : myPlayer.worms){
                if (enemyWorm.health > 0 && myWorm.health > 0){
                    jarak2.add(euclideanDistance(enemyWorm.position.x,enemyWorm.position.y,myWorm.position.x,myWorm.position.y)) ;
                } else {
                    jarak2.add(999999999);
                }
            }
            noWormTerdekat.add(jarak2.indexOf(Collections.min(jarak2)));
        }
        if (noWormTerdekat.get(0)==noWormTerdekat.get(1) && noWormTerdekat.get(1)==noWormTerdekat.get(2)){
            Worm inDanger = myPlayer.worms[noWormTerdekat.get(0)];
            System.out.println(inDanger.profession);
            return inDanger;
        } else {
            System.out.println("Enemy still not target specific worm");
            return null;
        }

    }

    Command toOtherWorm (){
    //  Mendekat ke worm teman (yang paling dekat)
        Worm otherWorm = findClosestFriendWorm(gameState);
        if (otherWorm != null){
            Direction toOther = resolveDirection(currentWorm.position, otherWorm.position);
            Cell SelectedBlock = gameState.map[currentWorm.position.y + toOther.y][currentWorm.position.x + toOther.x];
            if (SelectedBlock.type == CellType.AIR) {
                return new MoveCommand(SelectedBlock.x, SelectedBlock.y);
            } else if (SelectedBlock.type == CellType.DIRT) {
                return new DigCommand(SelectedBlock.x, SelectedBlock.y);
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public Command run() {
        if (gameState.currentRound >= 20 && currentWorm == PredictTargetedWorm() && wormAlone(currentWorm)){
            Command toOther = toOtherWorm();
            if(toOther != null){
                return toOther;
            }
        }
        Command toCentre = GetToCentre();
        if (toCentre != null){
            return toCentre;
        }
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0) {
                String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
                if (cells.contains(enemyPosition)) {
                    return enemyWorm;
                }
            }
        }

        return null;
    }

    private Worm getClosestOpponent(){
        Worm resultWorm = null;
        int minimum = 999999;
        for(Worm enemyWorm : opponent.worms){
            if(enemyWorm.health >0 ){
                int distance = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);
                if(distance < minimum) {
                    minimum = distance;
                    resultWorm = enemyWorm;
                }
            } else {
                continue;
            }
        }
        return resultWorm;
    }
    private Worm getLowestHealthOpponent(){
        Worm resultWorm = null;
        int minimumHealth = 999999;
        for(Worm enemyWorm : opponent.worms) {
            if ((enemyWorm.health > 0) && (enemyWorm.health < minimumHealth)) {
                minimumHealth = enemyWorm.health;
                resultWorm = enemyWorm;
            } else {
                continue;
            }
        }
        return resultWorm;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
}
