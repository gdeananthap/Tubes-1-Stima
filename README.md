# STIMA GREEDY BOT - ENTELLECT 2019 WORMS
Tugas Besar 1 IF2211 Strategi Algoritma Semester II Tahun 2020/2021 Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan “Worms”

## Requirements
1. [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), JDK 8 recommended and don't forget to set the path (environment variable).
2. [IntelliJ IDEA](https://www.jetbrains.com/idea/)
3. [Node JS](https://nodejs.org/en/download/)

## How To Build
1. Download latest Entellect Challenge 2019 [starter-pack](https://github.com/EntelectChallenge/2019-Worms/releases/tag/2019.3.2).
2. Clone this repository, if you didn't.
3. Copy the **java** folder inside **src** folder from this repository, then paste in **starter-pack/starter-bots**. 
   (*discalimer : if there is exist java folder inside the starter-bots, erase the existing java folder first*)
4. Open java folder with Intellij IDEA.
5. Open up the "Maven Projects" tab on the right side of the screen. From here go to the  **"java-sample-bot > Lifecycle** group and double-click **"Install"**. This  will create a .jar file in the folder called **target**. The file will be called "java-sample-bot-jar-with-dependencies.jar".

## How To Run
1. Update game-runner-config.json. Change "player-a" to "./starter-bots/java".
2. Go back to starter-pack folder.
3. Click run.bat

## How To Visualize
1. Download [Entellect Challenge 2019 Visualizer](https://github.com/dlweatherhead/entelect-challenge-2019-visualiser)
2. Copy your latest match log that you want to visualize from **match-logs** folder to "Matches" folder inside the visualizer folder that you have downloaded.
3. Click start-visualizer.
4. Choose your match log in the visualizer.
5. Have fun!

## Greedy Algorithm
### 1. *Greedy by Health Point*
Greedy by Health Point adalah strategi penargetan worm lawan berdasarkan health point. Pada setiap ronde, pilihlah worm lawan yang masih hidup dan memiliki health point terkecil dari seluruh worm lawan yang tersisa. Kemudian lakukan command yang “menargetkan” worm lawan tersebut.
### 2. *Greedy by Enemy Location*
Greedy by enemy location adalah strategi penargetan worm lawan berdasarkan jarak worm lawan dengan worm kita. Pada setiap ronde, cari worm lawan terdekat dengan worm kita yang sedang aktif, apabila worm lawan bisa diserang maka lakukan command shoot, apabila tidak maka dekati worm lawan.
### 3. *Greedy by Enemy Location V2*
Greedy by enemy location adalah strategi pelarian diri dari worm lawan dengan mempertimbangkan lokasi lawan. Untuk setiap ronde, jika worm kita dapat melakukan move command ke cell di sekitar maka pilih cell dengan jarak euclidean terjauh dari semua worm lawan. Jika worm tidak bisa melakukan move command, maka lakukan dig command ke cell di sekitar dengan jarak euclidean terjauh dari semua worm lawan.
### 4. *Greedy by Lava Location*
Greedy by Lava Location adalah strategi untuk menghindari lava yang muncul di peta. Lava muncul pada ronde 100 dan semakin lama semakin ketengah hingga menyisakan kotak kecil dengan radius 4 pada ronde 350. Untuk mengindari lava maka worm juga harus menghindari daerah di sekitar lava karena lava mengisi daerah di sekitarnya secara tiba-tiba, namun apabila pertandingan sudah berada diatas ronde 320 kita tidak perlu mengindari daerah sekitar lava karena itu hanya akan mengurangi daerah untuk lari apabila sedang bertempur dengan worm lawan. Apabila sedang tidak disekitar lava maka greedy ini tidak perlu dilakukan.
### 5. *Greedy by Bomb Area*
Greedy by bomb area adalah strategi untuk memaksimalkan damage bomb ke worm lawan tanpa perlu memberi damage ke worm kita. Pada setiap ronde dengan worm yang sedang aktif bertipe Agent dan masih memiliki banana bomb, pilih cell yang bukan bertipe deep space dan masih termasuk dalam jangkauan banana bomb dengan jumlah damage terbesar untuk semua worm lawan tapi tidak memberikan damage ke worm kita. 
### 6. *Greedy by Freezed Area*
Greedy by freezed area adalah strategi untuk membekukan musuh sebanyak mungkin dengan kondisi tertentu. Pada setiap ronde dengan current worm yang memiliki profesi technologist dan masih memiliki snowball, pilih cell yang bukan bertipe deep space dan masih termasuk dalam jangkauan snowball dengan jumlah worm lawan yang berada pada radius snowball terbanyak, worm kita tidak berada pada radius snowball, dan ada worm lawan beku yang bisa di shoot pada ronde selanjutnya.
#
## Author - WeTheClown
1. Gde Anantha Priharsena / 13519026
2. Reihan Andhika Putra / 13519043
3. Reyhan Emyr Arrosyid / 13519167