package za.co.entelect.challenge;

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
            if (myWorm.health >0 && myWorm != currentWorm){
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
        if (SelectedBlock.type == CellType.AIR) {
            return new MoveCommand(SelectedBlock.x, SelectedBlock.y);
        } else if (SelectedBlock.type == CellType.DIRT) {
            return new DigCommand(SelectedBlock.x, SelectedBlock.y);
        } else {
            return null;
        }
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

    private boolean mustBattle(Worm enemyWorm){
        if (myPlayer.score < opponent.score){
            return  false;
        } else if (currentWorm.health < 20 ) {
            return  false;
        } else {
            return (currentWorm.health >= enemyWorm.health) ||(livingMyOwnWorm()-livingEnemy() >= -1 && currentWorm.health <= enemyWorm.health);
        }
    }

    private Command escapeFromDanger(){
        List<MoveCommand> safeMove = getAllSafeMoveCommand(getAllMoveCommand());
        List<DigCommand> safeDig = getAllSafeDigCommand(getAllDigCommand());
        MoveCommand notSafeMove = safestMoveCommand(getAllMoveCommand());
        List<Worm> shootedEnemyWorm = getAllShootedWorm();
        if (!safeMove.isEmpty()){
            return safestMoveCommand(safeMove);
        } else if (!shootedEnemyWorm.isEmpty()){
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm with desperate");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        } else if (!safeDig.isEmpty()){
            return safestDigCommand(safeDig);
        } else if(notSafeMove!=null){
            return notSafeMove;
        } else {
            System.out.println("Hmm i dont know what to do(?)");
            return new DoNothingCommand();
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

    private MoveCommand chooseMoveCommandToCenter(List<MoveCommand> moves) {
        MoveCommand move = null;
        int min = 999999999;
        for (MoveCommand cmd : moves) {
            if (euclideanDistance(cmd.getX(), cmd.getY(), 16, 16) < min) {
                min = euclideanDistance(cmd.getX(), cmd.getY(), 16, 16);
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

    private MoveCommand getSelectMoves(){
        if(myPlayer.remainSelect == 0){
            return null;
        }
        System.out.println("Remaining Select : " + myPlayer.remainSelect);

        List<MoveCommand> potentialMove = new ArrayList<>();
        for(Worm myWorm : myPlayer.worms){
            if (myWorm.health<0 || myWorm == currentWorm){
                continue;
            }
            if(myWorm.frozenTime > 0){
                continue;
            }
//            if(shouldEngage)
            List <MoveCommand> moves =  getAllSafeMoveCommand(getAllMoveCommand()); // harusnya valid moves(?)
            //tbc

        }

        return null;
    }

    private List<Worm> getAllShootedWorm() {
        Set<String> cells = constructFireDirectionLines(currentWorm, currentWorm.weapon.range)
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
        // Belum ditambahin konditional kalo musuh yang ke freeze bakal digebuk musuh
        if ((freezedTeammate == 0 && freezedEnemy >= livingEnemy()-1 ) ||(freezedTeammate == 0 && freezedEnemy >= 1 && gameState.currentRound>150)){
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

    private Set<Cell> getDangerousCells() {
        Set<Cell> cells = new HashSet<>();
        for (Worm enemyWorm : opponent.worms){
            if (enemyWorm.health > 0) {
                Position enemyPos = enemyWorm.position;
                List<Cell> curEnemyWormCells = constructFireDirectionLines(enemyWorm, 4)
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

                cells.addAll(curEnemyWormCells);
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

    public Command run() {

        Position center = new Position();
        center.x = 16;
        center.y = 16;
        Set<Cell> lavaAndAdjacent = getLavaAndAdjacent();
        List<CellandBombDamage> bombedLocation = getAllBombedLocation();
        List<CellandFreezeCount> freezedLocation = getFreezedLocation();
        List<Worm> shootedEnemyWorm = getAllShootedWorm();

        Set<Cell> dangerousCell = getDangerousCells();
        Cell myWormCell = gameState.map[currentWorm.position.y][currentWorm.position.x];

        // If our worm in lava or next to lava
        if (lavaAndAdjacent.contains(myWormCell)) {
            System.out.println("Our worm maybe in or next to lava");
            List<MoveCommand> nonLavaMoves = getAllMoveCommand();
            List<DigCommand> nonLavaDigs = getAllDigCommand();
            nonLavaMoves.removeIf(move -> lavaAndAdjacent.contains(gameState.map[move.getY()][move.getX()]));
            nonLavaDigs.removeIf(dig -> lavaAndAdjacent.contains(gameState.map[dig.getY()][dig.getX()]));

            if (!nonLavaMoves.isEmpty()) {
                System.out.println("Moving to center");
                return chooseMoveCommandToCenter(nonLavaMoves);
            }

            if (!nonLavaDigs.isEmpty()) {
                System.out.println("Digging to center");
                return chooseDigCommandToCenter(nonLavaDigs);
            }

            Command toCentre = moveOrDigTo(center);
            if (toCentre != null){
                System.out.println("Going straight to center");
                return toCentre;
            }
        }

        //  If our worm in danger
        if (dangerousCell.contains(myWormCell)){
            System.out.println("Our worm maybe in danger, choose what to do wisely");
            // Enemy maybe targeting our Agent
            if (!bombedLocation.isEmpty()){
                bombedLocation.sort(Comparator.comparing(CellandBombDamage::getDamageToEnemy).reversed());
                System.out.println("Agent is targeted, bomb enemy worm");
                return new BananaCommand(bombedLocation.get(0).cell.x,bombedLocation.get(0).cell.y);
                // Maybe we are targeted by banana(?)
            } else if (shootedEnemyWorm.isEmpty()){
                System.out.println("Escape that shit");
                return escapeFromDanger();
            } else {
                // The enemy that make us in danger are freezed
                if(shootedEnemyWorm.size()==1 && shootedEnemyWorm.get(0).frozenTime>0){
                    System.out.println("Attack that freezed enemy");
                    Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                    return new ShootCommand(direction);
                    // Is it okay to battle the enemy?
                } else if(shootedEnemyWorm.size()==1 && mustBattle(shootedEnemyWorm.get(0))) {
                    System.out.println("Attack with desperate");
                    Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                    return new ShootCommand(direction);
                } else {
                    System.out.println("Escape that shit");
                    return escapeFromDanger();
                }
            }
        }


        //  Attacking without danger

        if (!bombedLocation.isEmpty()){
            bombedLocation.sort(Comparator.comparing(CellandBombDamage::getDamageToEnemy).reversed());
            System.out.println("Bombing enemy worm");
            return new BananaCommand(bombedLocation.get(0).cell.x,bombedLocation.get(0).cell.y);
        }

        if (!freezedLocation.isEmpty()){
            freezedLocation.sort(Comparator.comparing(CellandFreezeCount::getFreezeCount).reversed());
            System.out.println("Freezing enemy worm");
            return new SnowballCommand(freezedLocation.get(0).cell.x,freezedLocation.get(0).cell.y);
        }

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
}
