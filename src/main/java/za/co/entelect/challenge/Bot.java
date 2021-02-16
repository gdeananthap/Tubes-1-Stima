package za.co.entelect.challenge;

import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import javax.sound.midi.Soundbank;
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
    private MyPlayer myPlayer;
    private MyWorm currentWorm;
    private int selected = -1;


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

    private MyWorm findClosestFriendWorm() {
        int distance = 1000000000;
        MyWorm selectedWorm = null;
        for(MyWorm myWorm : myPlayer.worms){
            if (myWorm.health >0 && myWorm.id != currentWorm.id){
                if (euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y) < distance){
                    distance = euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y);
                    selectedWorm =myWorm;
                }
            }
        }
        return selectedWorm;
    }

    private Command moveOrDigTo(Position pos) {
        // Mencoba gerak lurus ke pos
        Direction toPos = resolveDirection(currentWorm.position, pos);
        Cell SelectedBlock = gameState.map[currentWorm.position.y + toPos.y][currentWorm.position.x + toPos.x];
        if (SelectedBlock.type == CellType.AIR && SelectedBlock.occupier == null) {
            return new MoveCommand(SelectedBlock.x, SelectedBlock.y);
        } else if (SelectedBlock.type == CellType.DIRT) {
            return new DigCommand(SelectedBlock.x, SelectedBlock.y);
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
            if (Collections.min(jarak2) != 999999999){
                noWormTerdekat.add(jarak2.indexOf(Collections.min(jarak2)));
            }
        }
        int noTerdekat = noWormTerdekat.get(0);
        boolean diincar = true;
        for (int i = 0 ; i < noWormTerdekat.size(); i++){
            if (noWormTerdekat.get(i)!=noTerdekat){
                diincar = false;
            }
        }
        if (diincar){
            Worm inDanger = myPlayer.worms[noTerdekat];
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
            List<MoveCommand> allMove = getAllMoveCommand();
            Direction toOther = resolveDirection(currentWorm.position, target.position);
            Cell SelectedBlock = gameState.map[currentWorm.position.y + toOther.y][currentWorm.position.x + toOther.x];
            if (SelectedBlock.type == CellType.AIR && SelectedBlock.occupier != null) {
                return new MoveCommand(SelectedBlock.x, SelectedBlock.y, selected);
            } else if (SelectedBlock.type == CellType.DIRT) {
                return new DigCommand(SelectedBlock.x, SelectedBlock.y, selected);
            } else if (!allMove.isEmpty()) {
                return chooseMoveCommandToPosition(allMove, target.position);
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    private boolean mustBattle(Worm enemyWorm){
        if (myPlayer.score < opponent.score){
            return  false;
        } else if (currentWorm.health < 20 ) {
            return  false;
        } else {
            return (currentWorm.health >= enemyWorm.health) ||(livingMyOwnWorm() > livingEnemy()&& currentWorm.health <= enemyWorm.health);
        }
    }

    private Command escapeFromDanger(){
        Set<Cell> lavaAndAdjacent = getLavaAndAdjacent();
        List<MoveCommand> safeMove = getAllSafeMoveCommand(getAllMoveCommand());
        List<MoveCommand> nonLavaSafeMove = filterMoveByCells(safeMove, lavaAndAdjacent);
        List<DigCommand> safeDig = getAllSafeDigCommand(getAllDigCommand());
        List<Worm> shootedEnemyWorm = getAllShootedWorm(currentWorm);
        List<Worm> shootedAndFreezedEnemyWorm = getFreezedWorm(shootedEnemyWorm);
        Set<Cell> predictedDangerCells = getPredictedDangerousCells(false);
        List<MoveCommand> predictedSaveMoves = filterMoveByCells(filterMoveByCells(getAllMoveCommand(), lavaAndAdjacent), predictedDangerCells);

        if (!nonLavaSafeMove.isEmpty()) {
            return safestMoveCommand(nonLavaSafeMove);
        // Attack Freezed Worm
        } else if (!shootedAndFreezedEnemyWorm.isEmpty()){
            shootedAndFreezedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting freezed worm");
            Direction direction = resolveDirection(currentWorm.position, shootedAndFreezedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        } else if (!predictedSaveMoves.isEmpty()) {
            System.out.println("Moving to predicted safe cell");
            return safestMoveCommand(predictedSaveMoves);
        } else if (!shootedEnemyWorm.isEmpty()){
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm with desperate");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        } else if (!safeDig.isEmpty()){
            return safestDigCommand(safeDig);
        } else {
            return null;
        }
    }
    private List<MoveCommand> getAllMoveCommand(){
        List<MoveCommand> moveCommands = new ArrayList<>();
        List<Cell> surround = getSurroundingCells(currentWorm.position.x,currentWorm.position.y);
        for (Cell cell :surround){
            if (cell.type == CellType.AIR && cell.occupier == null){
                moveCommands.add(new MoveCommand(cell.x, cell.y));
            }
        }
        return moveCommands;
    }

    private List<MoveCommand> getAllSafeMoveCommand(List<MoveCommand> allMove){
        List<MoveCommand> safeMove = new ArrayList<>();
        Set<Cell> dangerousCell = getDangerousCells();
        for(MoveCommand move : allMove){
            Cell location = gameState.map[move.getY()][move.getX()];
            if (!dangerousCell.contains(location)){
                safeMove.add(move);
            }
        }
        return safeMove;
    }

    private MoveCommand safestMoveCommand(List<MoveCommand> safeMoves){
        MoveCommand safestMove = null;
        int range = -99999999;
        for (MoveCommand safeMove : safeMoves){
            int rangemove = 0;
            for (Worm enemyWorm : opponent.worms){
                if (enemyWorm.health>0){
                    rangemove += euclideanDistance(enemyWorm.position.x,enemyWorm.position.y, safeMove.getX(),safeMove.getY());
                }
                if (rangemove > range) {
                    range = rangemove;
                    safestMove = safeMove;
                }
            }
        }
        return  safestMove;
    }

    private MoveCommand chooseMoveCommandToPosition(List<MoveCommand> moves, Position pos) {
        MoveCommand move = null;
        int min = 999999999;
        for (MoveCommand cmd : moves) {
            if (euclideanDistance(cmd.getX(), cmd.getY(), pos.x, pos.y) < min) {
                min = euclideanDistance(cmd.getX(), cmd.getY(), pos.x, pos.y);
                move = cmd;
            }
        }
        return move;
    }

    private DigCommand chooseDigCommandToCenter(List<DigCommand> digs) {
        DigCommand dig = null;
        int min = 999999999;
        for (DigCommand cmd : digs) {
            if (euclideanDistance(cmd.getX(), cmd.getY(), 16, 16) < min) {
                min = euclideanDistance(cmd.getX(), cmd.getY(), 16, 16);
                dig = cmd;
            }
        }
        return dig;
    }

    private List<DigCommand> getAllDigCommand(){
        List<DigCommand> digCommands = new ArrayList<>();
        List<Cell> surround = getSurroundingCells(currentWorm.position.x,currentWorm.position.y);
        for (Cell cell :surround){
            if (cell.type == CellType.DIRT){
                digCommands.add(new DigCommand(cell.x, cell.y));
            }
        }
        return digCommands;
    }

    private List<DigCommand> getAllSafeDigCommand(List<DigCommand> allDig){
        List<DigCommand> safeDig = new ArrayList<>();
        Set<Cell> dangerousCell = getDangerousCells();
        for(DigCommand dig : allDig){
            Cell location = gameState.map[dig.getY()][dig.getX()];
            if (!dangerousCell.contains(location)){
                System.out.println("found safe dig");
                safeDig.add(dig);
            }
        }
        return safeDig;
    }

    private DigCommand safestDigCommand(List<DigCommand> safeDigs){
        DigCommand safestDig = null;
        int range = -99999999;
        for (DigCommand safeMove : safeDigs){
            int rangedig = 0;
            for (Worm enemyWorm : opponent.worms){
                if (enemyWorm.health>0){
                    rangedig += euclideanDistance(enemyWorm.position.x,enemyWorm.position.y, safeMove.getX(),safeMove.getY());
                }
                if (rangedig > range) {
                    range = rangedig;
                    safestDig = safeMove;
                }
            }
        }
        return safestDig;
    }

    private List<Worm> getAllShootedWorm(MyWorm worm) {
        Set<String> cells = constructFireDirectionLines(worm, worm.weapon.range)
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

    private List<Worm> getFreezedWorm(List<Worm> allShotedWorm ){
        List<Worm> freezedWorm = new ArrayList<>();
        for (Worm worm : allShotedWorm){
            if (worm.frozenTime>0){
                freezedWorm.add(worm);
            }
        }
        return freezedWorm;
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
                        damageToEnemy += 20-(directionMultiplier*7);
                    } else if (cell.occupier.playerId == myPlayer.id && cell.occupier.health>0)  {
                        damageToUs += 20-(directionMultiplier*7);
                    }
                }

            }
        }
        if (damageToUs == 0 && (damageToEnemy >= 26 ||(damageToEnemy >= 20 && livingEnemy() < 3))){
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
        Set<Integer> canShoot = new HashSet<>();
        Set<Integer> frozenEnemy = new HashSet<>();
        for (MyWorm myWorm : gameState.myPlayer.worms) {
            if (myWorm.health > 0) {
                canShoot.addAll(getAllShootedWorm(myWorm).stream().map(worm -> worm.id).collect(Collectors.toList()));
            }
        }
        if (freezedCell.occupier != null){
            if (freezedCell.occupier.playerId == opponent.id && freezedCell.occupier.health>0){
                if (freezedCell.occupier.frozenTime > 0){
                    return null;
                } else {
                    freezedEnemy += 1;
                    frozenEnemy.add(freezedCell.occupier.id);
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
                            frozenEnemy.add(cell.occupier.id);
                        }
                    } else if (cell.occupier.playerId == myPlayer.id && cell.occupier.health>0)  {
                        freezedTeammate += 1;
                    }
                }

            }
        }
        frozenEnemy.retainAll(canShoot);
        if (!frozenEnemy.isEmpty() && freezedTeammate == 0 &&((freezedEnemy >= livingEnemy()-1 ) ||(freezedEnemy >= 1 && gameState.currentRound>150))){
            return new CellandFreezeCount(freezedCell,freezedEnemy);
        } else {
            return  null;
        }
    }

    private Set<Cell> getLavaAndAdjacent() {
        Set<Cell> cells = new HashSet<>();
        for (Cell[] row : gameState.map) {
            for (Cell cell : row) {
                if (cell.type == CellType.LAVA) {
                    cells.add(cell);
                    List<Cell> surrounding = getSurroundingCells(cell.x, cell.y);
                    for (Cell surround : surrounding) {
                        cells.add(surround);
                    }
                }
            }
        }
        return cells;
    }

    private int livingEnemy(){
        int livingEnemy = 0;
        for(Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) {
                livingEnemy ++;
            }
        }
        return livingEnemy;
    }

    private int livingMyOwnWorm(){
        int livingWorm = 0;
        for(Worm myWorm : myPlayer.worms){
            if (myWorm.health>0) {
                livingWorm ++;
            }
        }
        return livingWorm;
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

    private List<MoveCommand> getSelectMove(){
        return null;
    }

    private Boolean isEnemyGather(){
        boolean isGather = false;
        if(livingEnemy() < 2){
            return false;
        }
        for(Worm enemyWorm : opponent.worms){
            if(enemyWorm.health <= 0 ){
                continue;
            }
            boolean isOtherWormClose = false;
            for(Worm otherEnemyWorm : opponent.worms){
                if(enemyWorm.id == otherEnemyWorm.id || otherEnemyWorm.health <= 0){
                    continue;
                }else{
                    int distance = euclideanDistance(enemyWorm.position.x, enemyWorm.position.y, otherEnemyWorm.position.x, otherEnemyWorm.position.y);
                    if(distance < 6){
                        isOtherWormClose = true;
                    }else{
                        isOtherWormClose = false;
                    }
                }
            }
            if(isOtherWormClose){
                isGather = true;
            }
        }
        return isGather;
    }

    private Set<Cell> getDangerousCells() {
        Set<Cell> cells = new HashSet<>();
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health > 0) {
                List<Cell> curEnemyWormCells = constructFireDirectionLines(enemyWorm, 4)
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

                cells.addAll(curEnemyWormCells);
            }
        }

        return cells;
    }

    private Set<Cell> getPredictedDangerousCells(boolean onlyCurrent) {
        Set<Cell> cells = new HashSet<>();
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health > 0) {
                List<List<Cell>> FireDirections = constructFireDirectionLines(enemyWorm, 4);
                if (enemyWorm.id == opponent.opponentCurrentWormId) {
                    for (List<Cell> fd : FireDirections) {
                        for (Worm worm : gameState.myPlayer.worms) {
                            if (worm.health > 0) {
                                if (fd.contains(gameState.map[worm.position.y][worm.position.x])) {
                                    cells.addAll(fd);
                                }
                            }
                        }
                    }
                } else {
                    if (!onlyCurrent) {
                        cells.addAll(FireDirections.stream().flatMap(List::stream).collect(Collectors.toList()));
                    }
                }
            }
        }

        return cells;
    }

    private List<List<Cell>> constructFireDirectionLines(Worm worm, int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = worm.position.x + (directionMultiplier * direction.x);
                int coordinateY = worm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(worm.position.x, worm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR && cell.type != CellType.LAVA) {
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
                if (!(i == x && j == y) && isValidCoordinate(i, j)) {
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

    public List<MoveCommand> filterMoveByCells(List<MoveCommand> commands, Set<Cell> cells) {
        return commands.stream().filter(move -> !cells.contains(gameState.map[move.getY()][move.getX()])).collect(Collectors.toList());
    }

    public Command run() {

        if(gameState.currentRound < 100){
            if(isEnemyGather()){
                System.out.println("ngumpul cok");
            }else{
                System.out.println("culun");
            }
        }

        Position center = new Position(16, 16);
        Set<Cell> lavaAndAdjacent = getLavaAndAdjacent();
        List<CellandBombDamage> bombedLocation = getAllBombedLocation();
        List<CellandFreezeCount> freezedLocation = getFreezedLocation();
        List<MoveCommand> allMove = getAllMoveCommand();
        List<DigCommand> allDig = getAllDigCommand();
        List<Worm> shootedEnemyWorm = getAllShootedWorm(currentWorm);

        Set<Cell> dangerousCell = getDangerousCells();
        Cell myWormCell = gameState.map[currentWorm.position.y][currentWorm.position.x];

        if (myPlayer.remainSelect > 0) {
            for (MyWorm myWorm : myPlayer.worms) {
                List<Worm> shot = getAllShootedWorm(myWorm);
                for (Worm shotWorm : shot) {
                    if (myWorm.health > 8 && myWorm.frozenTime == 0 && shotWorm.frozenTime > 1) {
                        return new ShootCommand(resolveDirection(myWorm.position, shotWorm.position), myWorm.id);
                    }
                }
            }
        }

        if (livingMyOwnWorm() == 1 && currentWorm.health <= 8) {
            System.out.println("Kaboor");
            Set<Cell> predictedCurrentEnemyShot = getPredictedDangerousCells(true);
            return chooseMoveCommandToPosition(filterMoveByCells(allMove, predictedCurrentEnemyShot), center);
        }

        // If our worm in lava or next to lava
        if (lavaAndAdjacent.contains(myWormCell) && (gameState.currentRound < 320 || !dangerousCell.contains(myWormCell))) {
            System.out.println("Our worm is in or next to lava");
            List<MoveCommand> nonLavaMoves = allMove.stream().filter(move -> !lavaAndAdjacent.contains(gameState.map[move.getY()][move.getX()])).collect(Collectors.toList());
            List<DigCommand> nonLavaDigs = allDig.stream().filter(dig -> !lavaAndAdjacent.contains(gameState.map[dig.getY()][dig.getX()])).collect(Collectors.toList());
            List<MoveCommand> nonLavaAndSaveMoves = getAllSafeMoveCommand(nonLavaMoves);

            if (!nonLavaAndSaveMoves.isEmpty()) {
                System.out.println("Moving to center while avoiding lava and danger");
                return chooseMoveCommandToPosition(nonLavaAndSaveMoves, center);
            }

            if (!nonLavaMoves.isEmpty()) {
                System.out.println("Moving to center while avoiding lava");
                return chooseMoveCommandToPosition(nonLavaMoves, center);
            }

            if (!allMove.isEmpty()) {
                System.out.println("Moving to center");
                return chooseMoveCommandToPosition(allMove, center);
            }

            if (!nonLavaDigs.isEmpty()) {
                System.out.println("Digging to center while avoiding lava");
                return chooseDigCommandToCenter(nonLavaDigs);
            }
        }

        //  Regardless of our worm in danger or not, if enemy is targetting out agent/technologist and we can bomb/freeze them with efficiency then bomb/freeze them
        //  Or if there are freezed worm nearby then shoot it

        // Enemies maybe targetting our agent or our agent found good spot to bomb
        if (!bombedLocation.isEmpty()){
            bombedLocation.sort(Comparator.comparing(CellandBombDamage::getDamageToEnemy).reversed());
            System.out.println("Bombing enemy worm");
            return new BananaCommand(bombedLocation.get(0).cell.x,bombedLocation.get(0).cell.y);
        }

        // Enemies maybe targetting our technologist or our technologist found good spot to freeze
        if (!freezedLocation.isEmpty()){
            freezedLocation.sort(Comparator.comparing(CellandFreezeCount::getFreezeCount).reversed());
            System.out.println("Freezing enemy worm");
            return new SnowballCommand(freezedLocation.get(0).cell.x,freezedLocation.get(0).cell.y);
        }

        //  If our worm in danger
        if (dangerousCell.contains(myWormCell)){
            System.out.println("Our worm maybe in danger, choose what to do wisely");
            // The enemy that make us in danger are freezed
            if(shootedEnemyWorm.size()==1 && shootedEnemyWorm.get(0).frozenTime>0){
                System.out.println("Attack that freezed enemy");
                Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                return new ShootCommand(direction);
                // Is it okay to battle the enemy?
            } else if(shootedEnemyWorm.size()==1 && mustBattle(shootedEnemyWorm.get(0))) {
                System.out.println("Attack with opportunity");
                Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                return new ShootCommand(direction);
            } else if(escapeFromDanger()!=null) {
                System.out.println("Escape that shit");
                return escapeFromDanger();
            }
        }


        //  Attacking without danger
        if (!shootedEnemyWorm.isEmpty()){
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        }

        if (currentWorm != PredictTargetedWorm() &&wormAlone()){
            Command toOther = toOtherWorm(PredictTargetedWorm());
            if(toOther != null){
                return toOther;
            }
        }

        // Kalo Gabut
        List<DigCommand> safeDig = getAllSafeDigCommand(getAllDigCommand());
        if(!safeDig.isEmpty()){
            System.out.println("Gabut jadi farming");
            return safestDigCommand(safeDig);
        } else{
            if(isEnemyGather() && livingMyOwnWorm() > 1){
                MyWorm closestFriend = findClosestFriendWorm();
                if(closestFriend != null && moveOrDigTo(closestFriend.position) != null){
                    System.out.println("Kabur ke teman terdekat");
                    return moveOrDigTo(closestFriend.position);
                }
            }else
                if(!isEnemyGather()) {
                Worm closestEnemy = getClosestOpponent();
                if (closestEnemy != null && moveOrDigTo(closestEnemy.position) != null) {
                    System.out.println("Deketin musuh terdekat");
                    return moveOrDigTo(closestEnemy.position);
                }
            }
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());
        System.out.println("Random ae");
        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR && block.occupier == null) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand();
    }
}
