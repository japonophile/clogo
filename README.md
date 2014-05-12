# Clogo

[Clogo](http://chopp.in/clj/clogo) is a LOGO REPL (interpreter) written in Clojure.
It can control a Lego NXT2 turtle robot or be used in standalone mode.

> Logo is an education programming language, designed in 1967 ... remembered mainly
> for its use of "turtle graphics" ... Logo is a multi-paradigm adaptation and 
> dialect of Lisp
-- [Wikipedia Logo (programming language)](<http://en.wikipedia.org/wiki/Logo_(programming_language)>)

## WTF? (What's This For?)

- For kids to learn programming, through the good old Logo turtle
- To mess up with Clojure, with many side effects (such as drawing on the screen 
  and even commanding a robot)
- To play with Lego Mindstorms and build a "turtle robot" which holds a pencil to
  draw on paper

## Quick Start (without the Lego Mindstorms robot)

You can try Clogo very easily.  Even if you don't have Lego Mindstorms, you can 
learn the LOGO language and command the turtle to draw on the screen.
All you need is either [leiningen](http://leiningen.org/) or 
[maven](http://maven.apache.org/) to build the app.

 ```git clone https://github.com/japonophile/clogo.git```

- With leiningen:

 ```lein compile
 lein uberjar
 java -jar target/clogo-0.1.0-SNAPSHOT-standalone.jar```

- With maven:

 ```mvn install
 java -jar target/clogo-0.1.0-SNAPSHOT-jar-with-dependencies.jar```

See the [cLOGO Crash Course](#clogo-crash-course) to find out what you can do with Clogo

## Instructions for building Clogo to control a Lego NXT2 turtle robot

If you plan to try with a Lego NXT2 robot, you will need
- Maven
- Lejos NXJ (Java for LEGO Mindstorms)
- Bluecove (bluetooth library to connect to the turtle robot)

Note: I am on Mac OSX and haven't tested on other platforms.

Here are the steps to follow:
1. Download Lejos librairies: get `leJOS_NXJ_0.9.1beta-3.tar.gz` from <http://sourceforge.net/projects/lejos/files/lejos-NXJ/>

2. Install them in your local Maven repo:

 ```mvn install:install-file -Dfile=leJOS_NXJ_0.9.1beta-3/lib/pc/pccomm.jar -DgroupId=lejos.pc -DartifactId=pccomm -Dversion=0.9.1-beta3 -Dpackaging=jar
 mvn install:install-file -Dfile=leJOS_NXJ_0.9.1beta-3/lib/nxt/classes.jar -DgroupId=lejos.nxt -DartifactId=classes -Dversion=0.9.1-beta3 -Dpackaging=jar```

3. Install and build bluecove (2.1.1-SNAPSHOT)

 ```git clone https://github.com/jarias/bluecove.git
 mvn install```

4. Use Maven to build the LOGO interpreter and the LogoTurtle software to be 
   downloaded onto the NXT2 robot.
   You need to specify the path of leJOS, because the compiler will be required
   to compile the turtle code.

 ```mvn install -Pnxt -Dnxj.home=/Users/yourhome/leJOS_NXJ_0.9.1beta-3```

5. Build your turtle robot.  It will use 2 motors to move and turn, and one motor
   to control the pencil.  Once the robot is built, you should configure bluetooth 
   and pair your NXT2 processor with your computer.

6. Download the LogoTurtle.nxd / LogoTurtle.nxj onto your NXT2 processor (using the
   USB link shipped with your Lego Mindstorms); then start your robot.  It will show
   the letters "connecting..." on its LCD screen.

6. Start the application on your computer.

 ```java -jar target/clogo-0.1.0-SNAPSHOT-jar-with-dependencies.jar```

   Within a few seconds, the application should try to connect with the turtle using
   bluetooth.  Each command to the LOGO REPL will be sent to the robot for execution.

Enjoy!

## cLOGO Crash Course

First of all, note that this LOGO interpreter only understand *French* LOGO commands
(although it should be straighforward to support other languages -contributions are
welcome!)

Here are some commands you can use:

#### Basic drawing commands

- `AVANCE X` (resp `RECULE X`) : move forward (resp backward) for _X_ steps
- `GAUCHE Y` (resp `DROITE X`) : make a left (resp right) turn of _X_ degrees
- `LEVECRAYON` (resp `BAISSECRAYON`) : put the pen down (resp up)
- `MONTRETORTUE` (resp `CACHETORTUE`) : show (resp hide) the turtle

#### Programming constructs

- `REPETE N [ ... ]` : repeat _N_ times the instructions in square brackets
- `POUR PROCNAME ... FIN` : defines a procedure called `PROCNAME` which can
  be reused by writing its name.  You can also pass parameters to the procedure
  like `POUR PROCNAME :PARAM1 ... FIN` where `:PARAM1` is an input parameter of
  the procedure which can then be used by writing `PARAM1` (without the ":" prefix).
- `DONNE VARNAME VALUE` : defines a variable `VARNAME` with a certain `VALUE` which
  can be an expression.

#### Expressions

- Clogo supports both prefix and infix operatorsr:
  - Prefix operators: `SOMME X Y`, `DIFFERENCE X Y`, `PRODUIT X Y` and `QUOTIENT X Y`.
  - Infix operators: `X + Y`, `X - Y`, `X * Y` and `X / Y`.
- You can also use parentheses in your expressions

#### Writing to the screen

- `ECRIS X` : write the value of _X_ to the screen
- Clogo has 2 predefined variables:
  - `POS` : the position of the turtle (x and y)
  - `CAP` : the orientation of the turtle (in degrees)

#### Examples:

- Draw a square

 ```POUR CARRE :COTE
   AVANCE COTE
   DROITE 90
 FIN```

- Draw a circle (actually a 36-sides polygon)

 ```POUR CERCLE
   REPETE 36 [ AVANCE 1 DROITE 10 ]
 FIN```

## Contributing

Clogo was an experiment / personal project to get my children play with
Lego Mindstorms and learn programming through the LOGO language.
It has only been tested on Mac (OSX Lion-Mavericks) and the turtle robot 
was an original creation (which also required some non-NXT bricks).

Feel free to try and contribute if you like Clogo.

Spreading the word by posting on Twitter or blogging about Clogo is also
a great way to show your appreciation.

## License

Copyright © 2014 Antoine Choppin

Distributed under the Eclipse Public License, the same as Clojure.

