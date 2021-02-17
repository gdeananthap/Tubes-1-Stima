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

    // Nested Class
    // Class yang memiliki atribut cell dan damage yang ditimbulkan jika banana bomb dilempar ke cell tersebut
    public class CellandBombDamage {
        //Atribut
        private Cell cell;
        private int damageToEnemy;
        //Constructor
        public CellandBombDamage(Cell x, int y){
            cell = x;
            damageToEnemy = y;
        }
        //Method
        public int getDamageToEnemy() {
            return damageToEnemy;
        }
    }

    // Class yang memiliki atribut cell dan jumlah worm yang akan beku jika snowball dilempar ke cell tersebut
    public class CellandFreezeCount {
        //Atribut
        private Cell cell;
        private int freezeCount;
        //Constructor
        public CellandFreezeCount(Cell x, int y){
            cell = x;
            freezeCount = y;
        }
        //Method
        public int getFreezeCount() {
            return freezeCount;
        }
    }

    // Atribut Bot
    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyPlayer myPlayer;
    private MyWorm currentWorm;
    private int selected = -1;

    //Constructor Bot
    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.myPlayer = gameState.myPlayer;
        this.currentWorm = getCurrentWorm(gameState);
    }

    // Method dengan return Boolean
    private boolean wormAlone(){
        // Method ini digunakan untuk mengetahui apakah ada worm kita yang sedang sendiri
        // sendiri didefinisikan jika jarak suatu worm dengan worm lainnya > 3
        boolean alone = true;
        for (Worm myWorm : myPlayer.worms){
            if (myWorm != currentWorm && myWorm.health>0 && euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y) <= 3){
                alone = false;
            }
        }
        return alone;
    }

    private boolean mustBattle(Worm enemyWorm){
        // Method ini digunakan untuk mengetahui apakah worm kita yang aktif harus melakukan pertarungan atau tidak
        if (myPlayer.score < opponent.score){
            return  false;
        } else if (currentWorm.health < 20 ) {
            return  false;
        } else {
            return (currentWorm.health >= enemyWorm.health) ||(livingMyOwnWorm() > livingEnemy()&& currentWorm.health <= enemyWorm.health);
        }
    }

    private boolean isEnemyGather(){
        // Method ini digunakan untuk mengetahui apakah semua worm lawan yang masih hidup sedang berkumpul atau berpencar
        boolean isGather = false;
        if(livingEnemy() < 2){
            return false;
        }
        for(Worm enemyWorm : opponent.worms){
            // Periksa apakah worm lawan masih hidup atau tidak
            if(enemyWorm.health <= 0 ){
                continue;
            }
            boolean isOtherWormClose = false;
            // Periksa jarak dengan worm lawan lain jika worm lain masih hidup
            for(Worm otherEnemyWorm : opponent.worms){
                if(enemyWorm.id == otherEnemyWorm.id || otherEnemyWorm.health <= 0){
                    continue;
                }else{
                    int distance = euclideanDistance(enemyWorm.position.x, enemyWorm.position.y, otherEnemyWorm.position.x, otherEnemyWorm.position.y);
                    // Jikaa jarak antar worm < 6 dapat dikatakan mereka berdekatan
                    if(distance < 6){
                        isOtherWormClose = true;
                    }else{
                        isOtherWormClose = false;
                    }
                }
            }
            // Jika semua worm berdekatan, maka dapat dikatakan semua worm lawan sedang berkumpul
            if(isOtherWormClose){
                isGather = true;
            }
        }
        return isGather;
    }

    private boolean isValidCoordinate(int x, int y) {
        // Method ini digunakan untuk mengecek apakah suatu koordinat valid dalam game ini
        // Suatu koordinat valid jika atribut 0<=X<33 an 0<=y<33 karena ukuran map sebesar (33x33)
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    // Method dengan return integer
    private int livingEnemy(){
        // Method ini digunakan untuk menghitung jumlah worm lawan yang masih hidup
        int livingEnemy = 0;
        for(Worm enemyWorm : opponent.worms){
            if (enemyWorm.health>0) {
                livingEnemy ++;
            }
        }
        return livingEnemy;
    }

    private int livingMyOwnWorm(){
        // Method ini digunakan untuk menghitung jumlah worm kita yang masih hidup
        int livingWorm = 0;
        for(Worm myWorm : myPlayer.worms){
            if (myWorm.health>0) {
                livingWorm ++;
            }
        }
        return livingWorm;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        // Method ini digunakan untuk menghitung jarak euclidean dari (aX, ay) ke (bX, bY) menggunakan rumus phytagoras
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    //Method dengan return MyWorm, Worm, List<Worm>
    private MyWorm getCurrentWorm(GameState gameState) {
        // Method ini digunakan untuk mengetahui worm kita yang sedang aktif
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private MyWorm findClosestFriendWorm() {
        // Method ini digunakan untuk mengetahui worm kita yang memiliki jarak terdekat dengan worm kita yang sedang aktif
        int distance = 1000000000;
        MyWorm selectedWorm = null;
        for(MyWorm myWorm : myPlayer.worms){
            // Cek apakah worm tersebut masih hidup dan bukan worm yang sedang aktif
            if (myWorm.health >0 && myWorm.id != currentWorm.id){
                // Cek jarak antar worm
                if (euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y) < distance){
                    distance = euclideanDistance(myWorm.position.x,myWorm.position.y,currentWorm.position.x,currentWorm.position.y);
                    selectedWorm =myWorm;
                }
            }
        }
        return selectedWorm;
    }

    private Worm getClosestOpponent(){
        // Method ini digunakan untuk mengetahui worm lawan yang memiliki jarak terdekat dengan worm kita yang sedang aktif
        Worm resultWorm = null;
        int minimum = 999999;
        for(Worm enemyWorm : opponent.worms){
            // Cek apakah worm lawan tersebut masih hidup
            if(enemyWorm.health >0 ){
                //Cek jarak
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
        // Method ini digunakan untuk mengetahui worm lawan yang masih hidup dengan health point terendah
        Worm resultWorm = null;
        int minimumHealth = 999999;
        for(Worm enemyWorm : opponent.worms) {
            // Cek apakah worm tersebut masih hidup dan health pointnya terendah
            if ((enemyWorm.health > 0) && (enemyWorm.health < minimumHealth)) {
                minimumHealth = enemyWorm.health;
                resultWorm = enemyWorm;
            } else {
                continue;
            }
        }
        return resultWorm;
    }

    private Worm PredictTargetedWorm(){
        // Method ini digunakan untuk mengecek apakah ada worm kita yang sedang diincar oleh lawan
        List<Integer> noWormTerdekat = new ArrayList<>();

        // Periksa apakah untuk setiap worm lawan sedang dekat dengan worm kita yang sama
        for (Worm enemyWorm : opponent.worms){
            // Untuk setiap worm kita, hitung jarak dari worm lawan ke worm kita
            List<Integer> jarak2 = new ArrayList<>();
            for (Worm myWorm : myPlayer.worms){
                if (enemyWorm.health > 0 && myWorm.health > 0){
                    // Jika kedua worm hidup, data setiap jarak antara worm kita dengan worm lawan
                    jarak2.add(euclideanDistance(enemyWorm.position.x,enemyWorm.position.y,myWorm.position.x,myWorm.position.y)) ;
                } else {
                    // nilai jarak 999999999 diinisialisasi jika minimal salah satu dari worm lawan atau
                    // worm kita yang sedang di cek jaraknya sudah tidak hidup
                    jarak2.add(999999999);
                }
            }
            // jika terdapat worm kita yang masih hidup yang dekat dengan worm lawan yang sedang di cek,
            // maka tambahkan ke list noWormTerdekat
            if (Collections.min(jarak2) != 999999999){
                noWormTerdekat.add(jarak2.indexOf(Collections.min(jarak2)));
            }
        }

        // Periksa apakah ID (dalam hal ini menggunakan index) worm kita yang dekat dengan worm lawan sama untuk setiap
        // pengecekan worm lawan, jika ketiganya berada dekat dengan worm yang sama maka bisa diprediksi bahwa
        // semua worm lawan sedang mengincar worm kita
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

    private List<Worm> getAllShootedWorm(MyWorm worm) {
        // Method ini digunakan untuk mendapatkan semua worm lawan yang berada pada radius shoot command dari setiap worm kita
        // Data semua kemungkinan cells yang berada pada radius weapon worm kita
        Set<String> cells = constructFireDirectionLines(worm, worm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        List<Worm> wormInRange = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0) {
                String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
                // Periksa apakah salah satu dari semua kemungkinan cells yang berada pada radius weapon worm kita
                // ditempati oleh worm lawan. Jika iya, tambahkan worm lawan tersebut ke List wormInRange.
                if (cells.contains(enemyPosition)) {
                    wormInRange.add(enemyWorm);
                }
            }
        }
        return wormInRange;
    }

    private List<Worm> getFreezedWorm(List<Worm> allEnemyWorm ){
        // Method ini digunakan untuk mendapatkan List of Worm lawan yang sedang beku
        List<Worm> freezedWorm = new ArrayList<>();
        for (Worm worm : allEnemyWorm){
            // Jika worm.frozenTime > 0 maka dapat dikatakan worm tersebut masih beku
            if (worm.frozenTime>0){
                freezedWorm.add(worm);
            }
        }
        return freezedWorm;
    }

    // Method dengan return Cell, List of Cell, Set of Cell
    // CellandBombDamage , List of CellandBombDamage, CellandFreezeCount, List of CellandFreezeCount

    private Set<Cell> getLavaAndAdjacent() {
        // Method ini digunakan untuk mendata semua cell yang bertipe lava dan cell yang berada disekitar cell bertipe lava
        Set<Cell> cells = new HashSet<>();
        // Lakukan pengecekan untuk setiap cell pada map
        for (Cell[] row : gameState.map) {
            for (Cell cell : row) {
                if (cell.type == CellType.LAVA) {
                    cells.add(cell);
                    // Data semua cell disekitar cell bertipe lava
                    List<Cell> surrounding = getSurroundingCells(cell.x, cell.y);
                    for (Cell surround : surrounding) {
                        cells.add(surround);
                    }
                }
            }
        }
        return cells;
    }

    private Set<Cell> getDangerousCells() {
        // Method ini digunakan untuk mengetahui semua cells yang berada pada jangkauan shoot semua worm lawan
        Set<Cell> cells = new HashSet<>();
        for (Worm enemyWorm : opponent.worms){
            // Hanya periksa jika worm lawan masih hidup
            if (enemyWorm.health > 0) {
                List<Cell> curEnemyWormCells = constructFireDirectionLines(enemyWorm, 4)
                        .stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                // Data semua cell yang berada pada jangkauan shoot sebuah worm lawan
                cells.addAll(curEnemyWormCells);
            }
        }
        return cells;
    }

    private Set<Cell> getPredictedDangerousCells(boolean onlyCurrent) {
        // Method ini digunakan untuk mengetahui semua cell yang diprediksikan berada pada jangkauan shoot
        // worm lawan yang aktif
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
                        // Hal ini hanya dilakukan untuk mengetahui semua cell yang diprediksikan berada pada jangkauan shoot
                        // semua worm lawan
                        cells.addAll(FireDirections.stream().flatMap(List::stream).collect(Collectors.toList()));
                    }
                }
            }
        }
        return cells;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        // Method ini digunakan untuk mendapatkan semua cell (8 arah mata angin) yang berada disekitar
        // sebuah cell yang sedang ditempati
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Jangan tambahkan cell yang sedang ditempati
                if (!(i == x && j == y) && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }
        return cells;
    }

    private List<List<Cell>> constructFireDirectionLines(Worm worm, int range) {
        // Method ini digunakan untuk mendapatkan semua cell yang berada dalam sebuah range,
        // bisa weapon, snowball atau banana bomb
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            // Tambahkan setiap cell yang berada pada satu garis lurus setiap direction dan
            // memiliki jarak <= range dari cell yang ditempati worm sekarang
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {
                int coordinateX = worm.position.x + (directionMultiplier * direction.x);
                int coordinateY = worm.position.y + (directionMultiplier * direction.y);

                // Hentikan increment jika koordinat sudah tidak valid
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                // Hentikan increment jika koordinat sudah berada diluar range
                if (euclideanDistance(worm.position.x, worm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }
                // Hentikan jika cell bertipe deep space
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

    private CellandBombDamage calculateBombDamage(Cell bombedCell){
        // Method ini digunakan untuk mengkalkulasi total bomb damage jika banana bomb dilempar ke sebuah cell
        int damageToUs = 0;
        int damageToEnemy = 0;
        // Jika terdapat worm pada cell tersebut, maka worm tersebut akan terkena 20 damage
        if (bombedCell.occupier != null){
            if (bombedCell.occupier.playerId == opponent.id && bombedCell.occupier.health>0){
                damageToEnemy += 20;
            } else if (bombedCell.occupier.playerId == myPlayer.id && bombedCell.occupier.health>0)  {
                damageToUs += 20;
            }
        }
        // Periksa untuk semua cell yang berada pada garis lurus untuk setiap direction dengan range banana bomb
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= currentWorm.bananaBomb.damageRadius; directionMultiplier++) {
                int coordinateX = bombedCell.x + (directionMultiplier * direction.x);
                int coordinateY = bombedCell.y + (directionMultiplier * direction.y);
                // Hentikan increment jika koordinat sudah tidak valid
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                // Hentikan increment jika koordinat sudah berada diluar range
                if (euclideanDistance(bombedCell.x, bombedCell.y, coordinateX, coordinateY) > currentWorm.bananaBomb.damageRadius) {
                    break;
                }
                Cell cell = gameState.map[coordinateY][coordinateX];
                // Jika dalam salah satu cell  terdapat worm, maka worm tersebut akan menerima damage lebih kecil
                // tergantung seberapa jauh jaraknya dari pusat bom. Disini kami asumsikan damagenya berkurang 7
                // setiap penambahan 1 jarak secara euclidean.
                if (cell.occupier != null){
                    if (cell.occupier.playerId == opponent.id && cell.occupier.health>0){
                        damageToEnemy += 20-(directionMultiplier*7);
                    } else if (cell.occupier.playerId == myPlayer.id && cell.occupier.health>0)  {
                        damageToUs += 20-(directionMultiplier*7);
                    }
                }

            }
        }
        // Return cell hanya jika cell tersebut menghasilkan 0 damage ke worm kita dan (damage ke lawan seminimalnya 26
        // (ada worm lawan yang kena ditengah dan ada setidaknya 1 worm lawan lain yang terkena di pinggir area bomb)
        // atau damage ke lawan seminimalnya 20 (artinya minimal ada satu yang terkena dipusat bomb) jika worm lawan
        // yang hidup < 3)
        if (damageToUs == 0 && (damageToEnemy >= 26 ||(damageToEnemy >= 20 && livingEnemy() < 3))){
            return new CellandBombDamage(bombedCell,damageToEnemy);
        } else {
            return  null;
        }
    }

    private List<CellandBombDamage> getAllBombedLocation() {
        // Method ini digunakan untuk mendata semua kemungkinan cell yang mungkin dilempar banana bomb dan memiliki
        // damage yang memenuhi syarat pada method calculatedBombDamage
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

    private CellandFreezeCount calculateFreezeCount(Cell freezedCell){
        // Method ini digunakan untuk menghitung berapa total worm lawan yang akan beku
        // jika snowball dilemparkan ke suatu cell
        int freezedTeammate = 0;
        int freezedEnemy = 0;
        Set<Integer> canShoot = new HashSet<>();
        Set<Integer> frozenEnemy = new HashSet<>();
        // Dapatkan semua cell yang berada pada radius shoot semua worm kita yang  masih hidup
        for (MyWorm myWorm : gameState.myPlayer.worms) {
            if (myWorm.health > 0) {
                canShoot.addAll(getAllShootedWorm(myWorm).stream().map(worm -> worm.id).collect(Collectors.toList()));
            }
        }
        // Periksa apakah cell yang akan dilempar snowball memiliki worm
        if (freezedCell.occupier != null){
            if (freezedCell.occupier.playerId == opponent.id && freezedCell.occupier.health>0){
                // Jika sudah ada worm lawan yang sudah beku sebelumnya, jangan lempar snowball ke cell tersebut
                if (freezedCell.occupier.frozenTime > 0){
                    return null;
                } else {
                    // Jika worm lawan pada cell tersebut tidak beku, data ID dari worm tersebut
                    freezedEnemy += 1;
                    frozenEnemy.add(freezedCell.occupier.id);
                }
            } else if (freezedCell.occupier.playerId == myPlayer.id && freezedCell.occupier.health>0)  {
                freezedTeammate += 1;
            }
        }
        // Periksa untuk semua cell yang berada pada garis lurus untuk setiap direction dengan range snowball
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= currentWorm.snowballs.freezeRadius; directionMultiplier++) {
                int coordinateX = freezedCell.x + (directionMultiplier * direction.x);
                int coordinateY = freezedCell.y + (directionMultiplier * direction.y);
                // Hentikan increment jika koordinat sudah tidak valid
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                // Hentikan increment jika koordinat sudah berada diluar range
                if (euclideanDistance(freezedCell.x, freezedCell.y, coordinateX, coordinateY) > currentWorm.snowballs.freezeRadius) {
                    break;
                }
                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.occupier != null){
                    // Jika sudah ada worm lawan yang sudah beku sebelumnya, jangan lempar snowball ke cell tersebut
                    if (cell.occupier.playerId == opponent.id && cell.occupier.health>0){
                        if (cell.occupier.frozenTime > 0){
                            return null;
                        } else {
                            // Jika worm lawan pada cell tersebut tidak beku, data ID dari worm tersebut
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
        // Return cell hanya jika ada worm lawan yang bisa difreeze dan tidak ada worm kita yang berada pada radius freezenya
        // dan (jumlah dari worm lawan yang terkena minimal worm yang hidup -1 atau minimal ada 1 worm lawan yang bisa difreeze
        // jika snowball belum digunakan hingga ronde 150)
        if (!frozenEnemy.isEmpty() && freezedTeammate == 0 &&((freezedEnemy >= livingEnemy()-1 ) ||(freezedEnemy >= 1 && gameState.currentRound>150))){
            return new CellandFreezeCount(freezedCell,freezedEnemy);
        } else {
            return  null;
        }
    }

    private List<CellandFreezeCount> getFreezedLocation() {
        // Method ini digunakan untuk mendata semua kemungkinan cell yang mungkin dilempar snowball dan
        // memenuhi syarat pada method calculatedFreezeCount
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

    // Method dengan return Direction
    private Direction resolveDirection(Position a, Position b) {
        // Method ini digunakan untuk menentukan arah dari sebuah posisi
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

    // Method dengan return Command
    private Command moveOrDigTo(Position pos) {
        // Method ini digunakan untuk membuat sebuah worm kita bergerak lurus menuju suatu posisi
        // Periksa kearah manakah worm tersebut harus bergerak
        Direction toPos = resolveDirection(currentWorm.position, pos);
        Cell SelectedBlock = gameState.map[currentWorm.position.y + toPos.y][currentWorm.position.x + toPos.x];
        // Jika cell yang dituju bertipe air dan tidak ada worm di cell tersebut, maka move ke cell tersebut
        if (SelectedBlock.type == CellType.AIR && SelectedBlock.occupier == null) {
            return new MoveCommand(SelectedBlock.x, SelectedBlock.y);
        } else if (SelectedBlock.type == CellType.DIRT) {
            // Jika cell yang dituju bertipe dirt, maka dig cell tersebut
            return new DigCommand(SelectedBlock.x, SelectedBlock.y);
        } else {
            // Jangan lakukan apa-apa jika tidak bisa melakukan move atau dig
            return null;
        }
    }

    private Command toOtherWorm (Worm target){
        //  Method ini digunakan untuk membuat sebuah worm mendekati worm target
        if (target != null){
            // Data semua move command yang mungkin
            List<MoveCommand> allMove = getAllMoveCommand();
            // Periksa kearah manakah worm tersebut harus bergerak
            Direction toOther = resolveDirection(currentWorm.position, target.position);
            Cell SelectedBlock = gameState.map[currentWorm.position.y + toOther.y][currentWorm.position.x + toOther.x];
            // Jika cell yang dituju bertipe air dan tidak ada worm di cell tersebut, maka move ke cell tersebut
            if (SelectedBlock.type == CellType.AIR && SelectedBlock.occupier != null) {
                return new MoveCommand(SelectedBlock.x, SelectedBlock.y, selected);
            } else if (SelectedBlock.type == CellType.DIRT) {
                // Jika cell yang dituju bertipe dirt, maka dig cell tersebut
                return new DigCommand(SelectedBlock.x, SelectedBlock.y, selected);
            } else if (!allMove.isEmpty()) {
                // Jika ada kemungkinan selain bergerak lurus langsung, make move ke cell tersebut dan tetap menuju target
                return chooseMoveCommandToPosition(allMove, target.position);
            } else {
                // Jangan lakukan apa-apa jika tidak bisa melakukan move atau dig
                return null;
            }
        } else {
            return null;
        }

    }

    private Command escapeFromDanger(){
        // Data semua cell yang bertipe lava dan sekitarnya
        Set<Cell> lavaAndAdjacent = getLavaAndAdjacent();
        // Data semua move command yang aman
        List<MoveCommand> safeMove = getAllSafeMoveCommand(getAllMoveCommand());
        // Data semua move command yang aman ke cell yang bukan bertipe lava dan sekitarnya
        List<MoveCommand> nonLavaSafeMove = filterMoveByCells(safeMove, lavaAndAdjacent);
        // Data semua dig command yang aman
        List<DigCommand> safeDig = getAllSafeDigCommand(getAllDigCommand());
        // Data semua worm lawan yang berada di range weapon worm aktif
        List<Worm> shootedEnemyWorm = getAllShootedWorm(currentWorm);
        // Data semua worm lawan yang berada di range weapon worm aktif dan sedang beku
        List<Worm> shootedAndFreezedEnemyWorm = getFreezedWorm(shootedEnemyWorm);
        // Data semua cell yang diprediksi tidak aman dari serangan worm lawan
        Set<Cell> predictedDangerCells = getPredictedDangerousCells(false);
        // Data semua move command ke sebuah cell yang merupakan irisan dari semua move yang mungkin dengan cell
        // bertipe lava dan sekitarnya dan cell prediksi yang tidak aman dari serangan worm lawan
        List<MoveCommand> predictedSaveMoves = filterMoveByCells(filterMoveByCells(getAllMoveCommand(), lavaAndAdjacent), predictedDangerCells);

        // Periksa jenis bahaya
        if (!nonLavaSafeMove.isEmpty()) {
            // Keaada worm bahaya karena keberadaan lava
            // Jika bisa move command yang aman ke cell yang bukan bertipe lava dan sekitarnya
            // Move ke cell tersebut
            System.out.println("Move to Non Lava Cell");
            return safestMoveCommand(nonLavaSafeMove);
        } else if (!shootedAndFreezedEnemyWorm.isEmpty()){
            // Keadaan worm bahaya karena keberadaan worm lawan, tapi worm lawan tersebut beku
            shootedAndFreezedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting Freezed worm");
            Direction direction = resolveDirection(currentWorm.position, shootedAndFreezedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        } else if (!predictedSaveMoves.isEmpty()) {
            // Ada kemungkinan move ke cell yang diprediksi aman dari serangan lawan
            System.out.println("Moving to predicted safe cell");
            return safestMoveCommand(predictedSaveMoves);
        } else if (!shootedEnemyWorm.isEmpty()){
            // Ada musuh yang berada pada jangakauan weapon worm kita
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm with desperate");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        } else if (!safeDig.isEmpty()){
            // Terpaksa menerima damage dari worm lawan, tapi melakukan dig ke suatu cell yang aman dari serangan lawan
            return safestDigCommand(safeDig);
        } else {
            // Jangan lakukan apa-apa jika tidak memenuhi semua command diatas
            return null;
        }
    }

    // Move Command
    private MoveCommand safestMoveCommand(List<MoveCommand> safeMoves){
        // Method ini digunakan untuk mendapatkan move command dengan jarak terjauh dari worms lawan
        // dari semua kemungkinan safeMoves. Method ini digunakan untuk skema pelarian diri
        MoveCommand safestMove = null;
        int range = -99999999;
        for (MoveCommand safeMove : safeMoves){
            int rangemove = 0;
            for (Worm enemyWorm : opponent.worms){
                // Periksa apakan worm lawan masih hidup
                if (enemyWorm.health>0){
                    rangemove += euclideanDistance(enemyWorm.position.x,enemyWorm.position.y, safeMove.getX(),safeMove.getY());
                }
                // Periksa jarak terjauh dengan jarak yang diuji
                if (rangemove > range) {
                    range = rangemove;
                    safestMove = safeMove;
                }
            }
        }
        return  safestMove;
    }

    private MoveCommand chooseMoveCommandToPosition(List<MoveCommand> moves, Position pos) {
        // Method ini digunakan untuk mendapatkan move command dengan cell yang memiliki jarak terpendek ke lokasi pos
        MoveCommand move = null;
        int min = 999999999;
        for (MoveCommand cmd : moves) {
            // Periksa jarak antara cell pada command move dan cell dengan posisi pos
            if (euclideanDistance(cmd.getX(), cmd.getY(), pos.x, pos.y) < min) {
                min = euclideanDistance(cmd.getX(), cmd.getY(), pos.x, pos.y);
                move = cmd;
            }
        }
        return move;
    }

    private List<MoveCommand> getAllMoveCommand(){
        // Method ini digunakan untuk mendapatkan semua move command yang bisa dilakukan
        // ke surrounding cells dari worm kita yang aktif
        List<MoveCommand> moveCommands = new ArrayList<>();
        List<Cell> surround = getSurroundingCells(currentWorm.position.x,currentWorm.position.y);
        for (Cell cell :surround){
            // Jika cell berada disekitar worm aktif dan bertipe air maka tambahkan move command ke cell tersebut
            if (cell.type == CellType.AIR && cell.occupier == null){
                moveCommands.add(new MoveCommand(cell.x, cell.y));
            }
        }
        return moveCommands;
    }

    private List<MoveCommand> getAllSafeMoveCommand(List<MoveCommand> allMove){
        // Method ini digunakan untuk mendapatkan semua move command yang aman dari serangan worm lawan
        List<MoveCommand> safeMove = new ArrayList<>();
        Set<Cell> dangerousCell = getDangerousCells();
        for(MoveCommand move : allMove){
            Cell location = gameState.map[move.getY()][move.getX()];
            // Jika cell yang dituju menggunakan move command tidak termasuk cell yang berbahaya, maka tambahkan
            // move command ke cell tersebut
            if (!dangerousCell.contains(location)){
                safeMove.add(move);
            }
        }
        return safeMove;
    }

    public List<MoveCommand> filterMoveByCells(List<MoveCommand> commands, Set<Cell> cells) {
        // Method ini digunakan untuk melakukan filterisasi move command sehingga hanya menyisakan semua move command
        // yang menuju cell yang beririsan dengan Cells
        return commands.stream().filter(move -> !cells.contains(gameState.map[move.getY()][move.getX()])).collect(Collectors.toList());
    }

    // Dig Command
    private DigCommand chooseDigCommandToCenter(List<DigCommand> digs) {
        // Method ini digunakan untuk memilih 1 dig command dari semua dig command pada list digs
        // supaya worm kita bisa bergerak ke tengah dengan jarak seminimal mungkin
        DigCommand dig = null;
        int min = 999999999;
        for (DigCommand cmd : digs) {
            // Periksa apakah jarak cells yang akan di dig adalah cells dengan jarak minimum dari tengah map
            if (euclideanDistance(cmd.getX(), cmd.getY(), 16, 16) < min) {
                min = euclideanDistance(cmd.getX(), cmd.getY(), 16, 16);
                dig = cmd;
            }
        }
        return dig;
    }

    private DigCommand safestDigCommand(List<DigCommand> safeDigs){
        // Method ini digunakan untuk melakukan dig command dengan cells yang memiliki jarak terjauh dari semua worm lawan
        DigCommand safestDig = null;
        int range = -99999999;
        for (DigCommand safeMove : safeDigs){
            int rangedig = 0;
            for (Worm enemyWorm : opponent.worms){
                // Periksa apakah worm lawan masih hidup
                if (enemyWorm.health>0){
                    rangedig += euclideanDistance(enemyWorm.position.x,enemyWorm.position.y, safeMove.getX(),safeMove.getY());
                }
                // Periksa apakah jarak cell tersebut ke cell lawan merupakan cell terjauh dari worm lawan
                if (rangedig > range) {
                    range = rangedig;
                    safestDig = safeMove;
                }
            }
        }
        return safestDig;
    }

    private List<DigCommand> getAllDigCommand(){
        // Method ini digunakan untuk mendapatkaan semua dig command yang bisa
        // dilakukan pada surrounding cells dari worm kita yang sedang aktif
        List<DigCommand> digCommands = new ArrayList<>();
        List<Cell> surround = getSurroundingCells(currentWorm.position.x,currentWorm.position.y);
        for (Cell cell :surround){
            // Periksa apakah cell yang akan di dig bertipe dirt, jika iya maka tambah dig command ke cell tersebut
            if (cell.type == CellType.DIRT){
                digCommands.add(new DigCommand(cell.x, cell.y));
            }
        }
        return digCommands;
    }

    private List<DigCommand> getAllSafeDigCommand(List<DigCommand> allDig){
        // Method ini digunakan untuk mendapatkan semua dig command yang aman
        // dari jangkauan serangan worm lawan
        List<DigCommand> safeDig = new ArrayList<>();
        Set<Cell> dangerousCell = getDangerousCells();
        for(DigCommand dig : allDig){
            Cell location = gameState.map[dig.getY()][dig.getX()];
            // Periksa apakah cell yang akan di dig termasuk cell yang berada pada jangkauan serangan worm lawan
            if (!dangerousCell.contains(location)){
                System.out.println("found safe dig");
                safeDig.add(dig);
            }
        }
        return safeDig;
    }


    // Main Strategy
    public Command run() {
        // Persiapkan semua data yang dibutuhkan untuk melakukan berbagai strategi
        Position center = new Position(16, 16);
        Set<Cell> lavaAndAdjacent = getLavaAndAdjacent();
        List<CellandBombDamage> bombedLocation = getAllBombedLocation();
        List<CellandFreezeCount> freezedLocation = getFreezedLocation();
        List<MoveCommand> allMove = getAllMoveCommand();
        List<DigCommand> allDig = getAllDigCommand();
        List<Worm> shootedEnemyWorm = getAllShootedWorm(currentWorm);
        Set<Cell> dangerousCell = getDangerousCells();
        Cell myWormCell = gameState.map[currentWorm.position.y][currentWorm.position.x];

        // Jika masih bisa melakukan select dan ada worm worm yang bisa diserang dengan shoot command
        // Maka select worm kita yang bisa melakukan shoot ke worm lawan tersebut
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

        // Jika worm kita yang hidup tersisa 1 dan darah <= 8, hindari pertarungan
        if (livingMyOwnWorm() == 1 && currentWorm.health <= 8) {
            System.out.println("Escape from enemy");
            Set<Cell> predictedCurrentEnemyShot = getPredictedDangerousCells(true);
            return chooseMoveCommandToPosition(filterMoveByCells(allMove, predictedCurrentEnemyShot), center);
        }

        // Jika worm kita berada di cell bertipe lava atau berada disekitar lava
        if (lavaAndAdjacent.contains(myWormCell) && (gameState.currentRound < 320 || !dangerousCell.contains(myWormCell))) {
            System.out.println("Our worm is in or next to lava");
            List<MoveCommand> nonLavaMoves = filterMoveByCells(allMove, lavaAndAdjacent);
            List<DigCommand> nonLavaDigs = allDig.stream().filter(dig -> !lavaAndAdjacent.contains(gameState.map[dig.getY()][dig.getX()])).collect(Collectors.toList());
            List<MoveCommand> nonLavaAndSaveMoves = getAllSafeMoveCommand(nonLavaMoves);

            // Move ke Non Lava cell yang aman dari jangakauan serangan worm lawan dan menuju ke tengah map
            if (!nonLavaAndSaveMoves.isEmpty()) {
                System.out.println("Moving to center while avoiding lava and danger");
                return chooseMoveCommandToPosition(nonLavaAndSaveMoves, center);
            }

            // Move ke Non Lava Cell dan menuju ke tengah map
            if (!nonLavaMoves.isEmpty()) {
                System.out.println("Moving to center while avoiding lava");
                return chooseMoveCommandToPosition(nonLavaMoves, center);
            }

            // Move ke cell yang menuju ke tengah map tanpa memperdulikan lava atau jangkauan serangan worm lawan
            if (!allMove.isEmpty()) {
                System.out.println("Moving to center");
                return chooseMoveCommandToPosition(allMove, center);
            }

            // Tidak bisa melakukan move
            // Lakukan dig ke cell yang mengarah ke tengah dan menghindari lava
            if (!nonLavaDigs.isEmpty()) {
                System.out.println("Digging to center while avoiding lava");
                return chooseDigCommandToCenter(nonLavaDigs);
            }
        }

        // Jika lawan sedang menargetkan serangan ke worm kita secara spesifika antara agent atau technologist
        // Worm lawan sedang berkumpul dan bergerak ke arah agent, sehingga jika mereka sudah berada pada
        // range tembakan banana bomb maka enemy worm tersebut bisa di bomb selama agent kita masih memiliki banana bomb
        if (!bombedLocation.isEmpty()){
            bombedLocation.sort(Comparator.comparing(CellandBombDamage::getDamageToEnemy).reversed());
            System.out.println("Throwing banana bomb to enemy worm");
            return new BananaCommand(bombedLocation.get(0).cell.x,bombedLocation.get(0).cell.y);
        }
        // Worm lawan sedang berkumpul dan bergerak ke arah technologist, sehingga jika mereka sudah berada pada
        // range tembakan snowball maka enemy worm tersebut bisa di freeze selama technologist kita masih memiliki snowball
        if (!freezedLocation.isEmpty()){
            freezedLocation.sort(Comparator.comparing(CellandFreezeCount::getFreezeCount).reversed());
            System.out.println("Throwing snowball to enemy worm");
            return new SnowballCommand(freezedLocation.get(0).cell.x,freezedLocation.get(0).cell.y);
        }

        // Jika Worm kita yang aktif sedang dalam keaadaan bahaya
        if (dangerousCell.contains(myWormCell)){
            System.out.println("Our worm maybe in danger, choose what to do wisely");
            if(shootedEnemyWorm.size()==1 && shootedEnemyWorm.get(0).frozenTime>0){
                // Jika worm lawan yang membuat worm kita dalam keadaan bahaya sedang beku, serang worm tersebut
                System.out.println("Attack that freezed enemy");
                Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                return new ShootCommand(direction);
            } else if(shootedEnemyWorm.size()==1 && mustBattle(shootedEnemyWorm.get(0))) {
                // Jika worm kita harus melakukan pertarungan, maka lakukan pertarungan
                System.out.println("Attack with opportunity");
                Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
                return new ShootCommand(direction);
            } else if(escapeFromDanger()!=null) {
                // Jika worm kita harus melarikan diri dari bahaya tersebut
                System.out.println("Escaping from danger");
                return escapeFromDanger();
            }
        }

        //  Jika worm kita yang aktif bisa melakukan serangan ke worm lawan tanpa bahaya,
        //  maka serang worm lawan yang berada pada jangkauan dengan health poin terkecil
        if (!shootedEnemyWorm.isEmpty()){
            shootedEnemyWorm.sort(Comparator.comparing(Worm::getHealth));
            System.out.println("Shooting worm");
            Direction direction = resolveDirection(currentWorm.position, shootedEnemyWorm.get(0).position);
            return new ShootCommand(direction);
        }

        // Jika worm kita yang aktif tidak sedang ditarget lawan, maka gerak ke worm kita yang lain
        if (currentWorm != PredictTargetedWorm() &&wormAlone()){
            Command toOther = toOtherWorm(PredictTargetedWorm());
            if(toOther != null){
                return toOther;
            }
        }

        // Tidak ada bahaya yang berarti, baik lava maupun worm lawan.
        // Tidak ada juga worm lawan yang bisa dilawan.
        List<DigCommand> safeDig = getAllSafeDigCommand(getAllDigCommand());
        if(!safeDig.isEmpty()){
            // Jika ada cell yang bisa di dig dan tidak berada pada jangkauan lawan maka dig ke cell tersebut
            System.out.println("Digging surrounding cell");
            return safestDigCommand(safeDig);
        } else{
            // Periksa apakah lawan sedang berkumpul atau tidak (ada kemungkinan lawan ingin mengepung worm kita)
            if(isEnemyGather() && livingMyOwnWorm() > 1){
                // Jika worm lawan sedang berkumpul dan worm kita masih lebih dari 1,
                // maka lakukan pelarian diri menuju worm kita yang lain supaya bisa menambah
                // kekuatan jika memang diperlukan adanya pertarungan nantinya
                MyWorm closestFriend = findClosestFriendWorm();
                if(closestFriend != null && moveOrDigTo(closestFriend.position) != null){
                    System.out.println("Escaping blockade to closest friend");
                    return moveOrDigTo(closestFriend.position);
                }
            }else
                // Jika worm lawan tidak sedang berkumpul, maka gerak ke lawan terdekat untuk melakukan penyerangan
                if(!isEnemyGather()) {
                Worm closestEnemy = getClosestOpponent();
                if (closestEnemy != null && moveOrDigTo(closestEnemy.position) != null) {
                    System.out.println("Moving to closest enemy");
                    return moveOrDigTo(closestEnemy.position);
                }
            }
        }

        // Tidak ada strategi yang bisa dilakukan
        // Melakukan randomisasi gerakan
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());
        System.out.println("Random Command");
        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR && block.occupier == null) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
        return new DoNothingCommand();
    }
}
