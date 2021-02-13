package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    public class CellandBombDamage {
        public CellandBombDamage(Cell x, int y){
            cell = x;
            damageToEnemy = y;
        }

        public int getDamageToEnemy() {
            return damageToEnemy;
        }

        private Cell cell;
        private int damageToEnemy;
    }

    public class CellandFreezeCount {
        public CellandFreezeCount(Cell x, int y){
            cell = x;
            freezeCount = y;
        }

        public int getFreezeCount() {
            return freezeCount;
        }

        private Cell cell;
        private int freezeCount;
    }

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

    boolean wormAlone(){
        boolean alone = true;
        for (Worm myWorm : myPlayer.worms){
            if (myWorm != currentWorm && myWorm.health>0 && euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y) <= 3){
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

    Command toOtherWorm (Worm target){
    //  Mendekat ke worm lain (yang paling dekat)
        if (target != null){
            Direction toOther = resolveDirection(currentWorm.position, target.position);
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
        Command toCentre = GetToCentre();
        if (toCentre != null){
            return toCentre;
        }

        //        Attacking Without Danger
        List<CellandBombDamage> bombedLocation = new ArrayList<>();
        bombedLocation= getAllBombedLocation();
        if (!bombedLocation.isEmpty()){
            bombedLocation.sort(Comparator.comparing(CellandBombDamage::getDamageToEnemy).reversed());
            System.out.println("Bombing enemy worm");
            return new BananaCommand(bombedLocation.get(0).cell.x,bombedLocation.get(0).cell.y);
        }

        List<CellandFreezeCount> freezedLocation = new ArrayList<>();
        freezedLocation= getFreezedLocation();
        if (!freezedLocation.isEmpty()){
            freezedLocation.sort(Comparator.comparing(CellandFreezeCount::getFreezeCount).reversed());
            System.out.println("Freezing enemy worm");
            return new SnowballCommand(freezedLocation.get(0).cell.x,freezedLocation.get(0).cell.y);
        }

        List<Worm> shootedEnemyWorm = new ArrayList<>();
        shootedEnemyWorm = getAllShootedWorm();
        if (!shootedEnemyWorm.isEmpty()){
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        }

        if (gameState.currentRound >= 20 && currentWorm != PredictTargetedWorm() &&wormAlone()){
            Command toOther = toOtherWorm(PredictTargetedWorm());
            if(toOther != null){
                return toOther;
            }
        }

//        if (enemyWorm != null) {
//            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
//            return new ShootCommand(direction);
//        }

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

    private List<Worm> getAllShootedWorm() {
        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        List<Worm> wormInRange = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0) {
                String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
                if (cells.contains(enemyPosition)) {
                    wormInRange.add(enemyWorm);
                }
            }
        }

        return wormInRange;
    }

    private List<CellandBombDamage> getAllBombedLocation() {
        List<CellandBombDamage> bombedLocation = new ArrayList<>();
        if(currentWorm.profession.equals("Agent")){
            if(currentWorm.bananaBomb.count > 0){
                for (Cell[] cell: gameState.map){
                    for(Cell bombedCell : cell){
                        if (bombedCell.type != CellType.DEEP_SPACE && euclideanDistance(currentWorm.position.x,currentWorm.position.y,bombedCell.x,bombedCell.y) <= currentWorm.bananaBomb.range){
                            CellandBombDamage newElement = calculateBombDamage(bombedCell);
                            if (newElement != null) {
                                bombedLocation.add(newElement);
                            }
                        }
                    }
                }
            }
        }
        return bombedLocation;
    }

    private CellandBombDamage calculateBombDamage(Cell bombedCell){
        int damageToUs = 0;
        int damageToEnemy = 0;
        if (bombedCell.occupier != null){
            if (bombedCell.occupier.playerId == opponent.id && bombedCell.occupier.health>0){
                damageToEnemy += 20;
            } else if (bombedCell.occupier.playerId == myPlayer.id && bombedCell.occupier.health>0)  {
                damageToUs += 20;
            }
        }
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= currentWorm.bananaBomb.damageRadius; directionMultiplier++) {
                int coordinateX = bombedCell.x + (directionMultiplier * direction.x);
                int coordinateY = bombedCell.y + (directionMultiplier * direction.y);
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                if (euclideanDistance(bombedCell.x, bombedCell.y, coordinateX, coordinateY) > currentWorm.bananaBomb.damageRadius) {
                    break;
                }
                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.occupier != null){
                    if (cell.occupier.playerId == opponent.id && cell.occupier.health>0){
                        damageToEnemy += 20-(directionMultiplier*5);
                    } else if (cell.occupier.playerId == myPlayer.id && cell.occupier.health>0)  {
                        damageToUs += 20-(directionMultiplier*5);
                    }
                }

            }
        }
        if (damageToUs == 0 && damageToEnemy >= 20){
            return new CellandBombDamage(bombedCell,damageToEnemy);
        } else {
            return  null;
        }
    }

    private List<CellandFreezeCount> getFreezedLocation() {
        List<CellandFreezeCount> freezedLocation = new ArrayList<>();
        if(currentWorm.profession.equals("Technologist")){
            if(currentWorm.snowballs.count > 0){
                for (Cell[] cell: gameState.map){
                    for(Cell freezedCell : cell){
                        if (freezedCell.type != CellType.DEEP_SPACE && euclideanDistance(currentWorm.position.x,currentWorm.position.y,freezedCell.x,freezedCell.y) <= currentWorm.snowballs.range){
                            CellandFreezeCount newElement = calculateFreezeCount(freezedCell);
                            if (newElement != null) {
                                freezedLocation.add(newElement);
                            }
                        }
                    }
                }
            }
        }
        return freezedLocation;
    }

    private CellandFreezeCount calculateFreezeCount(Cell freezedCell){
        int freezedTeammate = 0;
        int freezedEnemy = 0;
        if (freezedCell.occupier != null){
            if (freezedCell.occupier.playerId == opponent.id && freezedCell.occupier.health>0){
                if (freezedCell.occupier.frozenTime > 0){
                    return null;
                } else {
                    freezedEnemy += 1;
                }
            } else if (freezedCell.occupier.playerId == myPlayer.id && freezedCell.occupier.health>0)  {
                freezedTeammate += 1;
            }
        }
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= currentWorm.snowballs.freezeRadius; directionMultiplier++) {
                int coordinateX = freezedCell.x + (directionMultiplier * direction.x);
                int coordinateY = freezedCell.y + (directionMultiplier * direction.y);
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                if (euclideanDistance(freezedCell.x, freezedCell.y, coordinateX, coordinateY) > currentWorm.snowballs.freezeRadius) {
                    break;
                }
                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.occupier != null){
                    if (cell.occupier.playerId == opponent.id && cell.occupier.health>0){
                        if (cell.occupier.frozenTime > 0){
                            return null;
                        } else {
                            freezedEnemy += 1;
                        }
                    } else if (cell.occupier.playerId == myPlayer.id && cell.occupier.health>0)  {
                        freezedTeammate += 1;
                    }
                }

            }
        }
        int livingEnemy = 0;
        for(Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) {
                livingEnemy ++;
            }
        }
        // Belum ditambahin konditional kalo musuh yang ke freeze bakal digebuk musuh
        if ((freezedTeammate == 0 && freezedEnemy >= livingEnemy-1 ) ||(freezedTeammate == 0 && freezedEnemy >= 1 && gameState.currentRound>150)){
            return new CellandFreezeCount(freezedCell,freezedEnemy);
        } else {
            return  null;
        }
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
