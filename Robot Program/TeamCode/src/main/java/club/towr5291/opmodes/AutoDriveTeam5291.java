
package club.towr5291.opmodes;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.vuforia.HINT;
import com.vuforia.Image;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import club.towr5291.astarpathfinder.A0Star;
import club.towr5291.astarpathfinder.sixValues;
import club.towr5291.functions.AStarGetPathVer2;
import club.towr5291.functions.BeaconAnalysisOCV;
import club.towr5291.functions.Constants;
import club.towr5291.functions.FileLogger;
import club.towr5291.libraries.LibraryStateSegAuto;
import club.towr5291.robotconfig.HardwareArmMotors;
import club.towr5291.robotconfig.HardwareDriveMotors;
/*
TOWR 5291 Autonomous
Copyright (c) 2016 TOWR5291
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Written by Ian Haden October 2016
*/

@Autonomous(name="Ians Autonomous Drive", group="5291")
public class AutoDriveTeam5291 extends LinearOpMode
{
    //set up TAG for logging prefic, this info will appear first in every log statemend
    private static final String TAG = "AutoDriveTeam5291";

    //variable for pathvales when processing the A*pathfinder
    public AStarGetPathVer2 getPathValues = new AStarGetPathVer2();

    //The autonomous menu settings from the sharepreferences
    private SharedPreferences sharedPreferences;
    private String teamNumber;
    private String allianceColor;
    private String alliancePosition;
    private int delay;
    private String numBeacons;
    private String robotConfig;

    // Declare OpMode members.
    private HardwareDriveMotors robotDrive   = new HardwareDriveMotors();   // Use a Pushbot's hardware
    private HardwareArmMotors armDrive   = new HardwareArmMotors();   // Use a Pushbot's hardware

    private ElapsedTime     runtime = new ElapsedTime();

    //set up the variables for file logger and what level of debug we will log info at
    private FileLogger fileLogger;
    private int debug = 3;

    //set up range sensor variables
    //set up range sensor1
    byte[] range1Cache; //The read will return an array of bytes. They are stored in this variable
    I2cAddr RANGE1ADDRESS = new I2cAddr(0x14); //Default I2C address for MR Range (7-bit)
    private  static final int RANGE1_REG_START = 0x04; //Register to start reading
    private static final int RANGE1_READ_LENGTH = 2; //Number of byte to read
    private I2cDevice RANGE1;
    private I2cDeviceSynch RANGE1Reader;
    private double mdblRangeSensor1;

    //set up rangesensor 2
    byte[] range2Cache; //The read will return an array of bytes. They are stored in this variable
    I2cAddr RANGE2ADDRESS = new I2cAddr(0x18); //Default I2C address for MR Range (7-bit)
    private static final int RANGE2_REG_START = 0x04; //Register to start reading
    private static final int RANGE2_READ_LENGTH = 2; //Number of byte to read
    private I2cDevice RANGE2;
    private I2cDeviceSynch RANGE2Reader;
    private double mdblRangeSensor2;

    //set up colour sensor variables
    private ColorSensor colorSensor;    // Hardware Device Object
    private boolean colourError = false;

    // Line Sensors
    // Analogue Inputs from the DIM
    private double mdblInputLineSensor1;     // Input State
    private double mdblInputLineSensor2;     // Input State
    private double mdblInputLineSensor3;     // Input State
    private double mdblWhiteThreshold = 0.4; //  anything below 1.5 is white, anything above 3 is grey tile
    AnalogInput LineSensor1;          // Device Object
    AnalogInput LineSensor2;          // Device Object
    AnalogInput LineSensor3;          // Device Object

    //set up Gyro variables
    private boolean gyroError = false;
    private ModernRoboticsI2cGyro gyro;                 // Hardware Device Object
    private int mdblGyroXVal, mdblGyroYVal, mdblGyroZVal = 0;        // Gyro rate Values
    private int mdblGyroHeading = 0;                        // Gyro integrated heading
    private int mdblGyroAngleZ = 0;
    private boolean mdblGyroLastResetState = false;
    private boolean mdblGyroCurResetState  = false;
    final double GYRO_CORRECTION_MULTIPLIER = 0.9833;
    private double mdblTurnAbsoluteGyro;
    private double mdblGyrozAccumulated;
    private int mintStableCount;
    private String mstrWiggleDir;
    private double mdblPowerBoost;
    private int mintPowerBoostCount;
    private int mintGyroFromVuforia;
    private boolean mblnGryoResetVuforia;

    //set up robot variables
    private double     COUNTS_PER_MOTOR_REV;            // eg: TETRIX = 1440 pulses, NeveRest 20 = 560 pulses, NeveRest 40 =  1120, NeveRest 60 = 1680 pulses
    private double     DRIVE_GEAR_REDUCTION;            // This is < 1.0 if geared UP, Tilerunner is geared up
    private double     WHEEL_DIAMETER_INCHES;           // For figuring circumference
    private double     WHEEL_ACTUAL_FUDGE;              // Fine tuning amount
    private double     COUNTS_PER_INCH;
    private double     ROBOT_TRACK;                     //  distance between centerline of rear wheels robot will pivot on rear wheel of omni on front, 16.5 track is 103.67 inches full circle
    private double     COUNTS_PER_DEGREE;
    private double     WHEEL_TURN_FUDGE;
    private double     REVERSE_DIRECTION;               // determines which directin the robot runs when FW is positive or negative when commanded to move a direction

    //vuforia localisation variables
    OpenGLMatrix lastLocation = null;
    private double localisedRobotX;
    private double localisedRobotY;
    private double localisedRobotBearing;
    private boolean localiseRobotPos;

    //define each state for the step.  Each step should go through some of the states below
    // set up the variables for the state engine
    private int mintCurrentStep = 1;                                                       // Current Step in State Machine.
    private int mintCurrentAStarStep = 1;                                                  // Current Step in AStar State Machine.
    private stepState mintCurrentStepState;                                                // Current State Machine State.
    private stepState mintCurrentDriveState;                                               // Current State of Drive.
    private stepState mintCurrentDriveHeadingState;                                        // Current State of Drive Heading.
    private stepState mintCurrentTankTurnState;                                            // Current State of Tank Turn.
    private stepState mintCurrentPivotTurnState;                                           // Current State of Pivot Turn.
    private stepState mintCurrentRadiusTurnState;                                          // Current State of Radius Turn.
    private stepState mintCurrentDelayState;                                               // Current State of Delay (robot doing nothing)
    private stepState mintCurrentVuforiaState;                                             // Current State of Vuforia Localisation
    private stepState mintCurrentVuforiaMoveState;                                         // Current State of Vuforia Move
    private stepState mintCurrentVuforiaTurnState;                                         // Current State of Vuforia Turn
    private stepState mintCurrentBeaconColourState;                                        // Current State of Beacon Colour
    private stepState mintCurrentAttackBeaconState;                                        // Current State of Attack Beacon
    private stepState mint5291CurrentShootParticleState;                                   // Current State of Shooting Ball in Vortex
    private stepState mint11231CurrentShootParticleState;                                  // Current State of Shooting Ball in Vortex
    private stepState mintCurrentTankTurnGyroHeadingState;                                 // Current State of Tank Turn using Gyro
    private stepState mintCurrentTankTurnGyroBasicState;                                   // Current State of Tank Turn using Gyro Basic
    private stepState mint5291CurrentLineFindState;                                        // Current State of the special function to move forward until line is found
    private stepState mint5291GyroTurnEncoderState;                                        // Current State of the Turn function that take the Gyro as an initial heading

    private LEDState mint5291LEDStatus;                                                   // Flash the LED based on the status

    private enum stepState {
        STATE_INIT,
        STATE_START,
        STATE_RUNNING,
        STATE_PAUSE,
        STATE_COMPLETE,
        STATE_TIMEOUT,
        STATE_ERROR,
        STATE_FINISHED,
        STATE_ASTAR_PRE_INIT,
        STATE_ASTAR_INIT,
        STATE_ASTAR_RUNNING,
        STATE_ASTAR_ERROR,
        STATE_ASTAR_COMPLETE
    }

    private enum LEDState {
        STATE_ERROR,
        STATE_TEAM,
        STATE_MOVING,
        STATE_BEACON,
        STATE_SUCCESS,
        STATE_FINISHED
    }

    private boolean readyToCapture = false;

    //variable for the state engine, declared here so they are accessible throughout the entire opmode with having to pass them through each function
    private int mintStartPositionLeft1;                      //Left Motor 1  - start position of the robot in inches, starts from 0 to the end
    private int mintStartPositionLeft2;                      //Left Motor 2  - start position of the robot in inches, starts from 0 to the end
    private int mintStartPositionRight1;                     //Right Motor 1 - start position of the robot in inches, starts from 0 to the end
    private int mintStartPositionRight2;                     //Right Motor 2 - start position of the robot in inches, starts from 0 to the end
    private int mintStepLeftTarget1;                         //Left Motor 1   - encoder target position
    private int mintStepLeftTarget2;                         //Left Motor 2   - encoder target position
    private int mintStepRightTarget1;                        //Right Motor 1  - encoder target position
    private int mintStepRightTarget2;                        //Right Motor 2  - encoder target position
    private double mdblStepTimeout;                          //Timeout value ofthe step, the step will abort if the timeout is reached
    private double mdblStepSpeed;                            //When a move command is executed this is the speed the motors will run at
    private String mdblRobotCommand;                         //The command the robot will execute, such as move forward, turn right etc
    private double mdblRobotParm1;                           //First Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private double mdblRobotParm2;                           //Second Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private double mdblRobotParm3;                           //Third Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private double mdblRobotParm4;                           //Fourth Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private double mdblRobotParm5;                           //Fifth Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private double mdblRobotParm6;                           //Sixth Parameter of the command, not all commands have paramters, A*Star has parameters, where moveing does not
    private boolean mdblRobotStepComplete;                   //flag to write back to the step if we finish the step, currently not used
    private double mdblStepTurnL;                            //used when decoding the step, this will indicate if the robot is turning left
    private double mdblStepTurnR;                            //used when decoding the step, this will indicate if the robot is turning right
    private double mdblRobotTurnAngle;                       //used to determine angle the robot will turn
    private double mdblStepDistance;                         //used when decoding the step, this will indicate how far the robot is to move in inches
    //private double mdblArcLengthRadiusTurn;                  //used to calculate the arc length when doing a radius turn
    private double mdblArcLengthRadiusTurnInner;             //used to calculate the arc length when doing a radius turn
    private double mdblArcLengthRadiusTurnOuter;             //used to calculate the arc length when doing a radius turn
    private double mdblSpeedOuter;                           //used to calculate the speed of the outer wheels during the turn
    private double mdblSpeedInner;                           //used to calculate the speed of the inner wheels during the turn
    private boolean mblnParallel;                            //used to determine if next step will run in parallel - at same time
    private boolean mblnRobotLastPos;                        //used to determine if next step will run from end of last step or from encoder position
    private int mintLastEncoderDestinationLeft1;             //used to store the encoder destination from current Step
    private int mintLastEncoderDestinationLeft2;             //used to store the encoder destination from current Step
    private int mintLastEncoderDestinationRight1;            //used to store the encoder destination from current Step
    private int mintLastEncoderDestinationRight2;            //used to store the encoder destination from current Step
    private boolean mblnNextStepLastPos;                     //used to detect using encoders or previous calc'd position
    private int mintStepDelay;                               //used when decoding the step, this will indicate how long the delay is on ms.
    private boolean mblnDisableVisionProcessing = false;     //used when moving to disable vision to allow faster speed reading encoders.
    private boolean mblnBaseStepComplete = false;
    private boolean mblnArmStepComplete = true;
    private ElapsedTime mStateTime = new ElapsedTime();     // Time into current state, used for the timeout

    //hashmap for the steps to be stored in.  A Hashmap is like a fancy array
    private HashMap<String,LibraryStateSegAuto> autonomousSteps = new HashMap<String,LibraryStateSegAuto>();
    private HashMap<String,String> powerTable = new HashMap<String,String>();

    //OpenCV Stuff
    private BeaconAnalysisOCV beaconColour = new BeaconAnalysisOCV();
    private Mat tmp = new Mat();
    private int mintCaptureLoop = 0;
    private int mintNumberColourTries = 0;
    private Constants.BeaconColours mColour;

    //variable for the counter when loading steps into the hashap, each step must have a unique step ID
    private int loadStep = 1;

    //servos
    // the servos are on the servo controller
    final static double SERVOLIFTRIGHT_MIN_RANGE  = 0;
    final static double SERVOLIFTRIGHT_MAX_RANGE  = 1.0;
    final static double SERVOLIFTLEFT_MIN_RANGE  = 0;
    final static double SERVOLIFTLEFT_MAX_RANGE  = 1.0;

    final static double SERVOBEACONRIGHT_MIN_RANGE  = 0;
    final static double SERVOBEACONRIGHT_MAX_RANGE  = 1.0;
    final static double SERVOBEACONLEFT_MIN_RANGE  = 0;
    final static double SERVOBEACONLEFT_MAX_RANGE  = 1.0;
    final static int SERVOBEACONLEFT_HOME = 7;
    final static int SERVOBEACONRIGHT_HOME = 4;

    private Servo servoLifterRight;
    private Servo servoLifterLeft;
    private Servo servoBeaconLeft;
    private Servo servoBeaconRight;

    //LED Strips
    DeviceInterfaceModule dim;                  // Device Object
    final int GREEN1_LED_CHANNEL = 0;
    final int RED1_LED_CHANNEL = 1;
    final int BLUE1_LED_CHANNEL = 2;
    final int GREEN2_LED_CHANNEL = 3;
    final int RED2_LED_CHANNEL = 4;
    final int BLUE2_LED_CHANNEL = 5;
    final boolean LedOn = false;
    final boolean LedOff = true;

    private double mdblLastOn;
    private double mdblLastOff;
    private boolean mblnLEDON;
    private int mintCounts = 0;


    //each robot speeds up and slows down at different rates
    //helps reduce over runs and
    //table for the tilerunner from AndyMark.  These values are for the twin 20 motors which makes the robot fast
    private void loadPowerTableTileRunner ()
    {
        powerTable.put(String.valueOf(0.5), ".1");
        powerTable.put(String.valueOf(1), ".2");
        powerTable.put(String.valueOf(2), ".3");
        powerTable.put(String.valueOf(4), ".4");
        powerTable.put(String.valueOf(6), ".5");
        powerTable.put(String.valueOf(8), ".6");
        powerTable.put(String.valueOf(10), ".7");
        powerTable.put(String.valueOf(12), ".8");
    }

    //table for the custom tanktread robot.  These values are for the twin 40 motors
    private void loadPowerTableTankTread ()
    {
        powerTable.put(String.valueOf(0.5), ".3");
        powerTable.put(String.valueOf(1), ".3");
        powerTable.put(String.valueOf(2), ".4");
        powerTable.put(String.valueOf(4), ".5");
        powerTable.put(String.valueOf(6), ".5");
        powerTable.put(String.valueOf(8), ".6");
        powerTable.put(String.valueOf(10), ".6");
        powerTable.put(String.valueOf(12), ".6");
        powerTable.put(String.valueOf(15), ".8");
    }


    //**********************************************************
    //   _____ ___   ___  __
    //  | ____|__ \ / _ \/_ |
    //  | |__    ) | (_) || |
    //  |___ \  / / \__, || |
    //     _) |/ /_   / / | |
    //  |____/|____| /_/  |_|
    //
    //  the code below is for team 5291 autonomous
    //**********************************************************
    private void loadStaticSteps5291 ()
    {
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
        //loadSteps(10, "LTE45", false, false,   0,    0,    0,    0,    0,    0,    0.6);   //test Left Turn using Encoder
        //loadSteps(15, "FWE96", false, true ,   1,   90,  0.04,   0,    0,    0,    0.55);  //forward with encoder and gyro at heading 90 degrees with error of 0.04
        //loadSteps(15, "FWE96", false, true ,   1,   90,  0.04,   0,    0,    0,    0.55);
    }

    private void loadStaticStepsRedRight5291 ()
    {
        // Valid Commands Angle
        // DEL = Delay in Microseconds
        // RTE = Right Turn Angle - Encoder
        // LTE = Left Turn Angle - Encoder
        // LPE = Left Pivot Angle - Encoder
        // RPE = Left Pivot Angle - Encoder
        // LRE = Left turn with radius - Encoder
        // RRE = Right turn with radius - Encoder
        // FWE = Drive Forward Distance - Encoder
        // REV = Drive Backward Distance - Encoder
        // ASE = AutoStar From Current Pos to X,Y - Encoder
        // FNC = Special Function
        // VFL = Vuforia Localise
        // VME = Vuforia Move - Encoder
        // VTE = Vuforia Turn - Encoder
        // ATB = Attack the Beacon
        // STB = Shoot The Ball
        // BCL = Get the beacon Colour
        // Red Beacon 1 = (32, 36), (-1016, 914)
        // Red Beacon 2 = (32, 84), (-1016, -305)
        // Blue Beacon 1 = (36,32), (-914, 1016) <270
        // Blue Beacon 2 = (84,32), (305, 1016) <270
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %

        loadSteps(15, "FWE-32", false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(10, "ST1" ,   false, false, 2500, 0,    0,    0,    0,    0,    0  );
        loadSteps(15, "FWE-24", false, false, 0,    0,    0,    0,    0,    0,    0.6);

        //loadSteps(30, "AS",  false, false,  0,    0,    0,   12,   24,  270,  0.5);
        //loadSteps(10, "VM",  false, false,  0,    0,    0,  -1016, -305,  180,   0.6);

        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0", false, false, 0,    0,    0,    0,    0,    0,    0);
    }

    private void loadStaticStepsRedLeft5291 ()
    {
        // Valid Commands Angle
        // DEL = Delay in Microseconds
        // RTE = Right Turn Angle - Encoder
        // LTE = Left Turn Angle - Encoder
        // LPE = Left Pivot Angle - Encoder
        // RPE = Left Pivot Angle - Encoder
        // LRE = Left turn with radius - Encoder
        // RRE = Right turn with radius - Encoder
        // FWE = Drive Forward Distance - Encoder (Parm 1 and 4, 1 = Gyro, 2 = Line Detect Stop, 3 = Range Sensor, Parm2 3 5 6 are parms for the Gyro, Range and Line Detect Stop)
        // REV = Drive Backward Distance - Encoder
        // ASE = AutoStar From Current Pos to X,Y - Encoder
        // FNC = Special Function
        // VFL = Vuforia Localise
        // VME = Vuforia Move - Encoder
        // VTE = Vuforia Turn - Encoder  Parm 1 is the desired heading
        // ATB = Attack the Beacon
        // STB = Shoot The Ball
        // BCL = Get the beacon Colour
        // Red Beacon 1 = (32, 36), (-1016, 914)
        // Red Beacon 2 = (32, 84), (-1016, -305)
        // Blue Beacon 1 = (36,32), (-914, 1016) <270
        // Blue Beacon 2 = (84,32), (305, 1016) <270
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %

        //testing starting at a 45 degree angle from wall
        loadSteps(4, "FWE34", false, true,   1,  225,  0.04,    0,    0,    0,    0.55);  //Forward using gyro to track at bearing 225 degrees, 0.04 gain on error
        loadSteps(4, "RRE45", false, true,   18,    0,    0,    0,    0,    0,    0.55);  //Right Turn with Radius of 18 inches
        loadSteps(4, "SF1  ", false, false,  19,    0,    0,    0,    0,    0,    0.3);   //SF1  is move forward slowly until line is found then stop
        loadSteps(3, "LTE85", false, false,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(4, "ST1" ,  false, false, 2500,   0,    0,    0,    0,    0,    0 );
        loadSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0);
        loadSteps(10, "ATB",  false, false,   0,    0,    0,    0,    0,    0,    0);
        loadSteps(3, "RTE85", false, true,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(6, "FWE46", false, true,   1,   271,  0.04,   0,    0,    0,    0.55);  //Forward using gyro to track at bearing 225 degrees, 0.04 gain on error
        loadSteps(4, "SF1  ", false, false,  19,    0,    0,    0,    0,    0,    0.3);   //SF1  is move forward slowly until line is found then stop
        loadSteps(3, "LTE85", false, false,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0);
        loadSteps(10, "ATB",  false, false,   0,    0,    0,    0,    0,    0,    0);
        loadSteps(3, "LTE130", false, true,   0,    0,    0,    0,    0,    0,    0.47);
        loadSteps(6, "FWE58", false, false,   1,   48,  0.02,   0,    0,    0,    0.55);  //Forward using gyro to track at bearing 225 degrees, 0.04 gain on error

        //testing starting back to the ball
        //loadSteps(4, "FWE6",  false, true,   1,   90,  0.04,   0,    0,    0,    0.55);
        //loadSteps(4, "LRE45", false, true,   18,    0,    0,    0,    0,    0,    0.55);
        //loadSteps(7, "FWE50", false, true ,   1,  135,  0.03,   0,    0,    0,    0.50);
        //loadSteps(7, "RTE45", false, true,    0,    0,    0,    0,    0,    0,    0.5);
        //loadSteps(7, "FWE6", false, false ,   1,   90,  0.04,   1,    0,    0,    0.45);
//        loadSteps(15, "FWE-5", false, false , 1,   90,  0.04,   1,    0,    0,    0.4);
        //loadSteps(3, "LTE90", false, false,   0,    0,    0,    0,    0,    0,    0.5);

        /*
        loadSteps(15, "FWE-14", false, false,  0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(10, "ST1",    false, false,  2500, 0,    0,    0,    0,    0,    0  );
        //loadSteps(10, "LTE40", false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(5, "GTH245",  false, false,  3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE-37", false, false,  0,    0,    0,    0,    0,    0,    0.6);
//        loadSteps(10, "RTE135", false, false, 0,    0,    0,    0,    0,    0,    0.7);
        loadSteps(6, "GTH0",    false, false,  3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(2, "DEL500",  false, false,  0,    0,    0,    0,    0,    0,    0);
        loadSteps(10, "VME",    false, false,  0,    0,    0,  -1150, -325,  0,   0.5);
        //loadSteps(5, "GTH0",  false, false, 0,    0,    0,    0,    0,    0,    0.3);
        //loadSteps(10, "FWE3",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
        //loadSteps(6, "GTH0",  false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(6, "GTH0",    false, false, 2,    0,    0,    0,    0,    0,    0.3);
        loadSteps(2, "DEL500",  false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(10,  "BCL",   false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(10,  "ATB",   false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(5, "GTH225",  false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE18",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(5, "GTH125",  false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE26",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
*/

//        loadSteps(10, "GTH90",  false, false, 0,    0,    0,    0,    0,    0,    0.3);
//        loadSteps(15, "FWE45",  false, false, 0,    0,    0,    0,    0,    0,    0.5);
//        loadSteps(10, "LTE88",  false, false, 0,    0,    0,    0,    0,    0,    0.3);
//        loadSteps(2, "DEL500",  false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(10, "VME",    false, false, 0,    0,    0,  -1150, 914,   0,   0.6);
//        loadSteps(10, "LTE25",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
//        loadSteps(2, "DEL500",  false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(10, "VTE",    false, false, 0,    0,    0,    0,    0,    0,   0.7);
//        loadSteps(2, "DEL500",  false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(10, "VTE",    false, false, 0,    0,    0,    0,    0,    0,   0.7);
//        loadSteps(10, "BCL",    false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(10, "ATB",    false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(10, "LTE140", false, false, 0,    0,    0,    0,    0,    0,    0);
//        loadSteps(15, "FWE60",  false, false, 0,    0,    0,    0,    0,    0,    0.6);

        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0", false, false,  0,    0,    0,    0,    0,    0,    0);
    }

    private void loadStaticStepsBlueLeft5291 ()
    {
        // Valid Commands Angle
        // DEL = Delay in Microseconds
        // RTE = Right Turn Angle - Encoder
        // LTE = Left Turn Angle - Encoder
        // LPE = Left Pivot Angle - Encoder
        // RPE = Left Pivot Angle - Encoder
        // LRE = Left turn with radius - Encoder
        // RRE = Right turn with radius - Encoder
        // FWE = Drive Forward Distance - Encoder
        // REV = Drive Backward Distance - Encoder
        // ASE = AutoStar From Current Pos to X,Y - Encoder
        // FNC = Special Function
        // VFL = Vuforia Localise
        // VME = Vuforia Move - Encoder
        // VTE = Vuforia Turn - Encoder
        // ATB = Attack the Beacon
        // STB = Shoot The Ball
        // BCL = Get the beacon Colour
        // Red Beacon 1 = (32, 36), (-1016, 914)
        // Red Beacon 2 = (32, 84), (-1016, -305)
        // Blue Beacon 1 = (36,32), (-914, 1016) <270
        // Blue Beacon 2 = (84,32), (305, 1016) <270
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %

        loadSteps(15, "FWE-32", false, false,  0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(10, "ST1" ,   false, false, 2500,  0,    0,    0,    0,    0,    0  );
        loadSteps(15, "FWE-24", false, false, 0,    0,    0,    0,    0,    0,    0.6);

        //loadSteps(30, "ASE",  false, false,  0,    0,    0,   12,   24,  270,  0.5);
        //loadSteps(10, "VME",  false, false,  0,    0,    0,  -1016, -305,  180,   0.6);

        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",  false, false, 0,    0,    0,    0,    0,    0,    0);
    }

    private void loadStaticStepsBlueRight5291 ()
    {
        // Valid Commands Angle
        // DEL = Delay in Microseconds
        // RTE = Right Turn Angle - Encoder
        // LTE = Left Turn Angle - Encoder
        // LPE = Left Pivot Angle - Encoder
        // RPE = Left Pivot Angle - Encoder
        // LRE = Left turn with radius - Encoder
        // RRE = Right turn with radius - Encoder
        // FWE = Drive Forward Distance - Encoder
        // REV = Drive Backward Distance - Encoder
        // ASE = AutoStar From Current Pos to X,Y - Encoder
        // FNC = Special Function
        // VFL = Vuforia Localise
        // VME = Vuforia Move - Encoder
        // VTE = Vuforia Turn - Encoder
        // ATB = Attack the Beacon
        // STB = Shoot The Ball
        // BCL = Get the beacon Colour
        // Red Beacon 1 = (32, 36), (-1016, 914)
        // Red Beacon 2 = (32, 84), (-1016, -305)
        // Blue Beacon 1 = (36,32), (-914, 1016) <270
        // Blue Beacon 2 = (84,32), (305, 1016) <270
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %

        loadSteps(15, "FWE-14", false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(10, "ST1" ,   false, false, 2500, 0,    0,    0,    0,    0,    0  );
        //loadSteps(10, "LTE40",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(5, "GTH215",  false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE-42", false, false, 0,    0,    0,    0,    0,    0,    0.6);
//        loadSteps(10, "RTE135", false, false,  0,    0,    0,    0,    0,    0,    0.7);
        loadSteps(6, "GTH90",  false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(2, "DEL500", false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(10, "VME",   false, false, 0,    0,    0,   320,  999,   0,    0.5);
        loadSteps(6, "GTH90",  false, false, 2,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE6",  false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(2, "DEL500", false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(10,  "BCL",  false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(10,  "ATB",  false, false, 0,    0,    0,    0,    0,    0,    0);
        loadSteps(5, "GTH235", false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE20", false, false, 0,    0,    0,    0,    0,    0,    0.6);
        loadSteps(5, "GTH135", false, false, 3,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "FWE34", false, false, 0,    0,    0,    0,    0,    0,    0.6);

        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",   false, false, 0,    0,    0,    0,    0,    0,    0);
    }


    //**********************************************************
    //    __ __ ___  ____   ___
    //   /_ /_ |__ \|___ \ / _ \
    //    | || |  ) | __) | | | |
    //    | || | / / |__ <| | | |
    //    | || |/ /_ ___) | |_| |
    //    |_||_|____|____/ \___/
    //
    //  the code below is for team 11230 autonomous
    //**********************************************************
    private void loadStaticSteps11230 ()
    {
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
    }
    private void loadStaticStepsRedLeft11230 ()
    {
    }
    private void loadStaticStepsRedRight11230 ()
    {
    }
    private void loadStaticStepsBlueLeft11230 ()
    {
    }
    private void loadStaticStepsBlueRight11230 ()
    {
    }

    //**********************************************************
    //         _  _ ___  ____   _
    //       /_ /_ |__ \|___ \/_ |
    //        | || |  ) | __) || |
    //        | || | / / |__ < | |
    //        | || |/ /_ ___) || |
    //        |_||_|____|____/ |_|
    //
    //  the code below is for team 11231 autonomous
    //**********************************************************
    private void loadStaticSteps11231 ()
    {
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
    }
    private void loadStaticStepsRedLeft11231 ()
    {

        // Valid Commands Angle
        // RT = Right Turn Angle
        // LT = Left Turn Angle
        // LP = Left Pivot Angle
        // RP = Left Pivot Angle
        // LR = Left turn with radius
        // RR = Right turn with radius
        // FW = Drive Forward Distance
        // RV = Drive Backward Distance
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
        loadSteps(10, "FWE9",   false, false, 0,    0,    0,    0,    0,    0,   0.3);     //
        loadSteps(7,  "ST2",    false, false, 4,    0,    0,    0,    0,    0,   0);            //Active Flicker
        loadSteps(10, "DEL5000",false, false, 0,    0,    0,    0,    0,    0,   0);
        loadSteps(10, "FWE11",  false, false, 0,    0,    0,    0,    0,    0,   0.6);     //
        loadSteps(10, "GTB-90", false, false, 0,    0,    0,    0,    0,    0,   0.5);     //
        loadSteps(10, "FWE13",  false, false, 0,    0,    0,    0,    0,    0,   0.6);     //
        loadSteps(10, "GTB-40", false, false, 0,    0,    0,    0,    0,    0,   0.5);     //
        loadSteps(5,  "FWE-18", false, false, 0,    0,    0,    0,    0,    0,   0.5);     //

        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",    false, false, 0,    0,    0,    0,    0,    0,   0);
    }

    private void loadStaticStepsRedRight11231 ()
    {

        // Valid Commands Angle
        // RT = Right Turn Angle
        // LT = Left Turn Angle
        // LP = Left Pivot Angle
        // RP = Left Pivot Angle
        // LR = Left turn with radius
        // RR = Right turn with radius
        // FW = Drive Forward Distance
        // RV = Drive Backward Distance
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
        loadSteps(10, "FWE28",  false, false,  0,    0,    0,    0,    0,    0,    0.3);     //
        loadSteps(10, "GTB40",  false, false,  0,    0,    0,    0,    0,    0,    0.5);     //
        loadSteps(7,  "ST2",    false, false,  4,    0,    0,    0,    0,    0,    0);            //Active Flicker
        loadSteps(10, "FWE40",  false, false,  0,    0,    0,    0,    0,    0,    .3);     //
        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",    false, false,  0,    0,    0,    0,    0,    0,    0);
    }

    private void loadStaticStepsBlueLeft11231 ()
    {
        // Valid Commands Angle
        // RT = Right Turn Angle
        // LT = Left Turn Angle
        // LP = Left Pivot Angle
        // RP = Right Pivot Angle
        // LR = Left turn with radius
        // RR = Right turn with radius
        // FW = Drive Forward Distance
        // RV = Drive Backward Distance
        // GT = Guro Turn at Set Degrees NEGATIVE NUMBER IS RIGHT and POSITIVE IS LEFT
        // BC = Push beacon color based on distance, 1 = BLUE Alliance and 2 = RED Alliance
        // FS = Active the FLICKER motor for x seconds
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
        loadSteps(10, "FWE28",  false, false, 0,    0,    0,    0,    0,    0,    0.3);
        loadSteps(10, "GTB-40", false, false, 0,    0,    0,    0,    0,    0,    0.5);
        loadSteps(7,  "ST2",    false, false, 4,    0,    0,    0,    0,    0,    0);            //Activate Flicker
        loadSteps(10, "FWE40",  false, false, 0,    0,    0,    0,    0,    0,    0.3);
        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",    false, false, 0,    0,    0,    0,    0,    0,    0);
    }

    private void loadStaticStepsBlueRight11231 ()
    {
        // Valid Commands Angle
        // RT = Right Turn Angle
        // LT = Left Turn Angle
        // LP = Left Pivot Angle
        // RP = Right Pivot Angle
        // LR = Left turn with radius
        // RR = Right turn with radius
        // FW = Drive Forward Distance
        // RV = Drive Backward Distance
        // GT = Guro Turn at Set Degrees NEGATIVE NUMBER IS RIGHT and POSITIVE IS LEFT
        // BC = Push beacon color based on distance, 1 = BLUE Alliance and 2 = RED Alliance
        // FS = Active the FLICKER motor for x seconds
        // BT = Touch the Beacon
        //           time, comm,  paral, lastp  parm, parm, parm, parm, parm, parm, powe
        //           out   and                   1     2     3     4     5     6     r
        //            s                                                              %
        loadSteps(10, "FWE9",  false, false, 0,    0,    0,    0,    0,    0,   0.3);     //
        loadSteps(7,  "ST2",   false, false, 4,    0,    0,    0,    0,    0,   0);            //Active Flicker
        loadSteps(10, "DL5000",false, false, 0,    0,    0,    0,    0,    0,   0);
        loadSteps(10, "FWE11", false, false, 0,    0,    0,    0,    0,    0,   0.6);     //
        loadSteps(10, "GTB90", false, false, 0,    0,    0,    0,    0,    0,   0.5);     //
        loadSteps(10, "FWE13", false, false, 0,    0,    0,    0,    0,    0,   0.6);     //
        loadSteps(10, "GTB40", false, false, 0,    0,    0,    0,    0,    0,   0.5);     //
        loadSteps(5,  "FWE-18",false, false, 0,    0,    0,    0,    0,    0,   0.5);     //
        //leave this delay of 0 in there, inserting a step doesn't work if inserting when at the last step
        loadSteps(2, "DEL0",   false, false, 0,    0,    0,    0,    0,    0,   0);
    }


    private void loadSteps(int timeOut, String command, boolean parallel, boolean lastPos, double parm1, double parm2, double parm3, double parm4, double parm5, double parm6, double power)
    {
        autonomousSteps.put(String.valueOf(loadStep), new LibraryStateSegAuto (loadStep, timeOut, command, parallel, lastPos, parm1, parm2, parm3, parm4, parm5, parm6, power, false));
        loadStep++;
    }

    private void insertSteps(int timeOut, String command, boolean parallel, boolean lastPos, double parm1, double parm2, double parm3, double parm4, double parm5, double parm6, double power, int insertlocation)
    {
        Log.d("insertSteps", "insert location " + insertlocation + " timout " + timeOut + " command " + command + " parallel " + parallel + " lastPos " + lastPos + " parm1 " + parm1 + " parm2 " + parm2 + " parm3 " + parm3 + " parm4 " + parm4 + " parm5 " + parm5 + " parm6 " + parm6 + " power " + power);
        HashMap<String,LibraryStateSegAuto> autonomousStepsTemp = new HashMap<String,LibraryStateSegAuto>();
        LibraryStateSegAuto processingStepsTemp;

        //move all the steps from current step to a temp location
        for (int loop = insertlocation; loop < loadStep; loop++)
        {
            processingStepsTemp = autonomousSteps.get(String.valueOf(loop));
            Log.d("insertSteps", "Reading all the next steps " + loop + " timout " + processingStepsTemp.getmRobotTimeOut() + " command " + processingStepsTemp.getmRobotCommand() );
            autonomousStepsTemp.put(String.valueOf(loop), autonomousSteps.get(String.valueOf(loop)));
        }
        Log.d("insertSteps", "All steps loaded to a temp hasmap");

        //insert the step we want
        autonomousSteps.put(String.valueOf(insertlocation), new LibraryStateSegAuto (loadStep, timeOut, command, parallel, lastPos, parm1, parm2, parm3, parm4, parm5, parm6, power, false));
        Log.d("insertSteps", "Inserted New step");

        //move all the other steps back into the sequence
        for (int loop = insertlocation; loop < loadStep; loop++)
        {
            processingStepsTemp = autonomousStepsTemp.get(String.valueOf(loop));
            Log.d("insertSteps", "adding these steps back steps " + (loop + 1) + " timout " + processingStepsTemp.getmRobotTimeOut() + " command " + processingStepsTemp.getmRobotCommand() );
            autonomousSteps.put(String.valueOf(loop + 1), autonomousStepsTemp.get(String.valueOf(loop)));
        }
        Log.d("insertSteps", "Re added all the previous steps");

        //increment the step counter as we inserted a new step
        loadStep++;
    }

    @Override
    public void runOpMode() throws InterruptedException
    {
        telemetry.addData("Init1     ",  "Starting!");
        telemetry.update();
        //
        // get a reference to a Modern Robotics DIM, and IO channels.
        dim = hardwareMap.get(DeviceInterfaceModule.class, "dim");   //  Use generic form of device mapping

        dim.setDigitalChannelMode(GREEN1_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel
        dim.setDigitalChannelMode(RED1_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel
        dim.setDigitalChannelMode(BLUE1_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel
        dim.setDigitalChannelMode(GREEN2_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel
        dim.setDigitalChannelMode(RED2_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel
        dim.setDigitalChannelMode(BLUE2_LED_CHANNEL, DigitalChannelController.Mode.OUTPUT); // Set the direction of each channel

        LedState(LedOn, LedOn, LedOn, LedOn, LedOn, LedOn);

        //load variables
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(hardwareMap.appContext);
        teamNumber = sharedPreferences.getString("club.towr5291.Autonomous.TeamNumber", "0000");
        allianceColor = sharedPreferences.getString("club.towr5291.Autonomous.Color", "Red");
        alliancePosition = sharedPreferences.getString("club.towr5291.Autonomous.Position", "Left");
        delay = Integer.parseInt(sharedPreferences.getString("club.towr5291.Autonomous.Delay", "0"));
        numBeacons = sharedPreferences.getString("club.towr5291.Autonomous.Beacons", "One");
        robotConfig = sharedPreferences.getString("club.towr5291.Autonomous.RobotConfig", "TileRunner-2x40");
        LibraryStateSegAuto processingSteps = new LibraryStateSegAuto(0,0,"",false,false,0,0,0,0,0,0,0,false);
        sixValues[] pathValues = new sixValues[1000];
        A0Star a0Star = new A0Star();
        String fieldOutput;
        HashMap<String,LibraryStateSegAuto> autonomousStepsAStar = new HashMap<>();

        if (debug >= 1)
        {
            fileLogger = new FileLogger(runtime);
            fileLogger.open();
            fileLogger.write("Time,SysMS,Thread,Event,Desc");
            fileLogger.writeEvent(TAG, "Log Started");
            Log.d(TAG, "Log Started");
            runtime.reset();
            telemetry.addData("FileLogger: ", runtime.toString());
            telemetry.addData("FileLogger Op Out File: ", fileLogger.getFilename());
        }

        //load all the vuforia stuff
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;

        switch (teamNumber) {
            case "5291":
                parameters.vuforiaLicenseKey = "AVATY7T/////AAAAGQJxfNYzLUgGjSx0aOEU0Q0rpcfZO2h2sY1MhUZUr+Bu6RgoUMUP/nERGmD87ybv1/lM2LBFDxcBGRHkXvxtkHel4XEUCsNHFTGWYcVkMIZqctQsIrTe13MnUvSOfQj8ig7xw3iULcwDpY+xAftW61dKTJ0IAOCxx2F0QjJWqRJBxrEUR/DfQi4LyrgnciNMXCiZ8KFyBdC63XMYkQj2joTN579+2u5f8aSCe8jkAFnBLcB1slyaU9lhnlTEMcFjwrLBcWoYIFAZluvFT0LpqZRlS1/XYf45QBSJztFKHIsj1rbCgotAE36novnAQBs74ewnWsJifokJGOYWdFJveWzn3GE9OEH23Y5l7kFDu4wc";
                break;
            case "11230":
                parameters.vuforiaLicenseKey = "Not Provided";
                break;
            case "11231":
                parameters.vuforiaLicenseKey = "Aai2GEX/////AAAAGaIIK9GK/E5ZsiRZ/jrJzdg7wYZCIFQ7uzKqQrMx/0Hh212zumzIy4raGwDY6Mf6jABMShH2etZC/BcjIowIHeAG5ShG5lvZIZEplTO+1zK1nFSiGFTPV59iGVqH8KjLbQdgUbsCBqp4f3tI8BWYqAS27wYIPfTK697SuxdQnpEZAOhHpgz+S2VoShgGr+EElzYMBFEaj6kdA/Lq5OwQp31JPet7NWYph6nN+TNHJAxnQBkthYmQg687WlRZhYrvNJepnoEwsDO3NSyeGlFquwuQwgdoGjzq2qn527I9tvM/XVZt7KR1KyWCn3PIS/LFvADSuyoQ2lsiOFtM9C+KCuNWiqQmj7dPPlpvVeUycoDH";
                break;
        }
        //parameters.cameraMonitorFeedback = VuforiaLocalizer.Parameters.CameraMonitorFeedback.AXES;

        VuforiaLocalizer vuforia = ClassFactory.createVuforiaLocalizer(parameters);
        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);                                          //enables RGB565 format for the image
        vuforia.setFrameQueueCapacity(1);                                                           //tells VuforiaLocalizer to only store one frame at a time
        //ConceptVuforiaGrabImage vuforia = new ConceptVuforiaGrabImage(parameters);
        Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS,4);

        VuforiaTrackables velocityVortex = vuforia.loadTrackablesFromAsset("FTC_2016-17");
        VuforiaTrackable wheels = velocityVortex.get(0);
        wheels.setName("wheels");  // wheels target

        VuforiaTrackable tools  = velocityVortex.get(1);
        tools.setName("tools");  // tools target

        VuforiaTrackable legos = velocityVortex.get(2);
        legos.setName("legos");  // legos target

        VuforiaTrackable gears  = velocityVortex.get(3);
        gears.setName("gears");  // gears target

        /** For convenience, gather together all the trackable objects in one easily-iterable collection */
        List<VuforiaTrackable> allTrackables = new ArrayList<VuforiaTrackable>();
        allTrackables.addAll(velocityVortex);

        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

        /**
         * We use units of mm here because that's the recommended units of measurement for the
         * size values specified in the XML for the ImageTarget trackables in data sets. E.g.:
         *      <ImageTarget name="stones" size="247 173"/>
         * You don't *have to* use mm here, but the units here and the units used in the XML
         * target configuration files *must* correspond for the math to work out correctly.
         */
        float mmPerInch        = 25.4f;
        float mmBotWidth       = 18 * mmPerInch;            // ... or whatever is right for your robot
        float mmFTCFieldWidth  = (12*12 - 2) * mmPerInch;   // the FTC field is ~11'10" center-to-center of the glass panels

        /**
         * In order for localization to work, we need to tell the system where each target we
         * wish to use for navigation resides on the field, and we need to specify where on the robot
         * the phone resides. These specifications are in the form of <em>transformation matrices.</em>
         * Transformation matrices are a central, important concept in the math here involved in localization.
         * See <a href="https://en.wikipedia.org/wiki/Transformation_matrix">Transformation Matrix</a>
         * for detailed information. Commonly, you'll encounter transformation matrices as instances
         * of the {@link OpenGLMatrix} class.
         *
         * For the most part, you don't need to understand the details of the math of how transformation
         * matrices work inside (as fascinating as that is, truly). Just remember these key points:
         * <ol>
         *
         *     <li>You can put two transformations together to produce a third that combines the effect of
         *     both of them. If, for example, you have a rotation transform R and a translation transform T,
         *     then the combined transformation matrix RT which does the rotation first and then the translation
         *     is given by {@code RT = T.multiplied(R)}. That is, the transforms are multiplied in the
         *     <em>reverse</em> of the chronological order in which they applied.</li>
         *
         *     <li>A common way to create useful transforms is to use methods in the {@link OpenGLMatrix}
         *     class and the Orientation class. See, for example, {@link OpenGLMatrix#translation(float,
         *     float, float)}, {@link OpenGLMatrix#rotation(AngleUnit, float, float, float, float)}, and
         *     {@link Orientation#getRotationMatrix(AxesReference, AxesOrder, AngleUnit, float, float, float)}.
         *     Related methods in {@link OpenGLMatrix}, such as {@link OpenGLMatrix#rotated(AngleUnit,
         *     float, float, float, float)}, are syntactic shorthands for creating a new transform and
         *     then immediately multiplying the receiver by it, which can be convenient at times.</li>
         *
         *     <li>If you want to break open the black box of a transformation matrix to understand
         *     what it's doing inside, use {@link MatrixF#getTranslation()} to fetch how much the
         *     transform will move you in x, y, and z, and use {@link Orientation#getOrientation(MatrixF,
         *     AxesReference, AxesOrder, AngleUnit)} to determine the rotational motion that the transform
         *     will impart. See {@link #format(OpenGLMatrix)} below for an example.</li>
         *
         * </ol>
         *
         * This example places the "stones" image on the perimeter wall to the Left
         *  of the Red Driver station wall.  Similar to the Red Beacon Location on the Res-Q
         *
         * This example places the "chips" image on the perimeter wall to the Right
         *  of the Blue Driver station.  Similar to the Blue Beacon Location on the Res-Q
         *
         * See the doc folder of this project for a description of the field Axis conventions.
         *
         * Initially the target is conceptually lying at the origin of the field's coordinate system
         * (the center of the field), facing up.
         *
         * In this configuration, the target's coordinate system aligns with that of the field.
         *
         * In a real situation we'd also account for the vertical (Z) offset of the target,
         * but for simplicity, we ignore that here; for a real robot, you'll want to fix that.
         *
         * To place the Wheels Target on the Red Audience wall:
         * - First we rotate it 90 around the field's X axis to flip it upright
         * - Then we rotate it  90 around the field's Z access to face it away from the audience.
         * - Finally, we translate it back along the X axis towards the red audience wall.
         *
         */

        // RED Targets
        // To Place GEARS Target
        // position is approximately - (-6feet, -1feet)

        OpenGLMatrix gearsTargetLocationOnField = OpenGLMatrix
                /* Then we translate the target off to the RED WALL. Our translation here
                is a negative translation in X.*/
                .translation(-mmFTCFieldWidth/2, -1 * 12 * mmPerInch, 0)
                .multiplied(Orientation.getRotationMatrix(
                        /* First, in the fixed (field) coordinate system, we rotate 90deg in X, then 90 in Z */
                        AxesReference.EXTRINSIC, AxesOrder.XZX,
                        AngleUnit.DEGREES, 90, 90, 0));
        gears.setLocation(gearsTargetLocationOnField);
        //RobotLog.ii(TAG, "Gears Target=%s", format(gearsTargetLocationOnField));

        // To Place GEARS Target
        // position is approximately - (-6feet, 3feet)
        OpenGLMatrix toolsTargetLocationOnField = OpenGLMatrix
                /* Then we translate the target off to the Blue Audience wall.
                Our translation here is a positive translation in Y.*/
                .translation(-mmFTCFieldWidth/2, 3 * 12 * mmPerInch, 0)
                //.translation(0, mmFTCFieldWidth/2, 0)
                .multiplied(Orientation.getRotationMatrix(
                        /* First, in the fixed (field) coordinate system, we rotate 90deg in X */
                        AxesReference.EXTRINSIC, AxesOrder.XZX,
                        AngleUnit.DEGREES, 90, 90, 0));
        tools.setLocation(toolsTargetLocationOnField);
        //RobotLog.ii(TAG, "Tools Target=%s", format(toolsTargetLocationOnField));

        //Finsih RED Targets

        // BLUE Targets
        // To Place LEGOS Target
        // position is approximately - (-3feet, 6feet)

        OpenGLMatrix legosTargetLocationOnField = OpenGLMatrix
                /* Then we translate the target off to the RED WALL. Our translation here
                is a negative translation in X.*/
                .translation(-3 * 12 * mmPerInch, mmFTCFieldWidth/2, 0)
                .multiplied(Orientation.getRotationMatrix(
                        /* First, in the fixed (field) coordinate system, we rotate 90deg in X, then 90 in Z */
                        AxesReference.EXTRINSIC, AxesOrder.XZX,
                        AngleUnit.DEGREES, 90, 0, 0));
        legos.setLocation(legosTargetLocationOnField);
        //RobotLog.ii(TAG, "Gears Target=%s", format(legosTargetLocationOnField));

        // To Place WHEELS Target
        // position is approximately - (1feet, 6feet)
        OpenGLMatrix wheelsTargetLocationOnField = OpenGLMatrix
                /* Then we translate the target off to the Blue Audience wall.
                Our translation here is a positive translation in Y.*/
                .translation(1 * 12 * mmPerInch, mmFTCFieldWidth/2, 0)
                .multiplied(Orientation.getRotationMatrix(
                        /* First, in the fixed (field) coordinate system, we rotate 90deg in X */
                        AxesReference.EXTRINSIC, AxesOrder.XZX,
                        AngleUnit.DEGREES, 90, 0, 0));
        wheels.setLocation(wheelsTargetLocationOnField);
        //RobotLog.ii(TAG, "Tools Target=%s", format(wheelsTargetLocationOnField));

        //Finsih BLUE Targets

        /**
         * Create a transformation matrix describing where the phone is on the robot. Here, we
         * put the phone on the right hand side of the robot with the screen facing in (see our
         * choice of BACK camera above) and in landscape mode. Starting from alignment between the
         * robot's and phone's axes, this is a rotation of -90deg along the Y axis.
         *
         * When determining whether a rotation is positive or negative, consider yourself as looking
         * down the (positive) axis of rotation from the positive towards the origin. Positive rotations
         * are then CCW, and negative rotations CW. An example: consider looking down the positive Z
         * axis towards the origin. A positive rotation about Z (ie: a rotation parallel to the the X-Y
         * plane) is then CCW, as one would normally expect from the usual classic 2D geometry.
         */
        OpenGLMatrix phoneLocationOnRobot = OpenGLMatrix
                .translation((mmBotWidth/2), 0, 300)
                .multiplied(Orientation.getRotationMatrix(
                        AxesReference.EXTRINSIC, AxesOrder.YZY,
                        AngleUnit.DEGREES, -90, 0, 0));
        //RobotLog.ii(TAG, "phone=%s", format(phoneLocationOnRobot));

        /**
         * Let the trackable listeners we care about know where the phone is. We know that each
         * listener is a {@link VuforiaTrackableDefaultListener} and can so safely cast because
         * we have not ourselves installed a listener of a different type.
         */
        ((VuforiaTrackableDefaultListener)gears.getListener()).setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);
        ((VuforiaTrackableDefaultListener)tools.getListener()).setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);
        ((VuforiaTrackableDefaultListener)legos.getListener()).setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);
        ((VuforiaTrackableDefaultListener)wheels.getListener()).setPhoneInformation(phoneLocationOnRobot, parameters.cameraDirection);

        /**
         * A brief tutorial: here's how all the math is going to work:
         *
         * C = phoneLocationOnRobot  maps   phone coords -> robot coords
         * P = tracker.getPose()     maps   image target coords -> phone coords
         * L = redTargetLocationOnField maps   image target coords -> field coords
         *
         * So
         *
         * C.inverted()              maps   robot coords -> phone coords
         * P.inverted()              maps   phone coords -> imageTarget coords
         *
         * Putting that all together,
         *
         * L x P.inverted() x C.inverted() maps robot coords to field coords.
         *
         * @see VuforiaTrackableDefaultListener#getRobotLocation()
         */
        telemetry.addData("Init2     ",  "Vuforia Options Loaded!");
        telemetry.update();

        //to add more config options edit strings.xml and AutonomousConfiguration.java
        switch (robotConfig) {
            case "TileRunner-2x40":   //Velocity Vortex Competition Base
                REVERSE_DIRECTION       = 1;                                                       // Reverse the direction without significant code changes, (using motor FORWARD REVERSE will affect the driver station as we use same robotconfig file
                COUNTS_PER_MOTOR_REV    = 1120 ;                                                    // eg: TETRIX = 1440 pulses, NeveRest 20 = 560 pulses, NeveRest 40 =  1120, NeveRest 60 = 1680 pulses
                DRIVE_GEAR_REDUCTION    = 0.7 ;                                                    // This is < 1.0 if geared UP, Tilerunner is geared up
                WHEEL_DIAMETER_INCHES   = 4.0 ;                                                     // For figuring circumference
                WHEEL_ACTUAL_FUDGE      = 1;                                                        // Fine tuning amount
                COUNTS_PER_INCH         = ((COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415)) * WHEEL_ACTUAL_FUDGE * REVERSE_DIRECTION;
                ROBOT_TRACK             = 16.5;                                                     //  distance between centerline of rear wheels robot will pivot on rear wheel of omni on front, 16.5 track is 103.67 inches full circle
                WHEEL_TURN_FUDGE        = 1.0;                                                        // Fine tuning amount
                COUNTS_PER_DEGREE       = (((2 * 3.1415 * ROBOT_TRACK) * COUNTS_PER_INCH) / 360) * WHEEL_TURN_FUDGE;
                loadPowerTableTileRunner();                                                         //load the power table
                break;
            case "5291 Tank Tread-2x40 Custom":   //for tank tread base
                REVERSE_DIRECTION       = 1;
                COUNTS_PER_MOTOR_REV    = 1120 ;                                                    // eg: TETRIX = 1440 pulses, NeveRest 20 = 560 pulses, NeveRest 40 =  1120, NeveRest 60 = 1680 pulses
                DRIVE_GEAR_REDUCTION    = 1.0 ;                                                     // Tank Tread is 1:1 ration
                WHEEL_DIAMETER_INCHES   = 3.75 ;                                                     // For figuring circumference
                WHEEL_ACTUAL_FUDGE      = 1;                                                        // Fine tuning amount
                COUNTS_PER_INCH         = ((COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415)) * WHEEL_ACTUAL_FUDGE * REVERSE_DIRECTION ;
                ROBOT_TRACK             = 18;                                                     //  distance between centerline of rear wheels robot will pivot on rear wheel of omni on front, 16.5 track is 103.67 inches full circle
                WHEEL_TURN_FUDGE        = 1.12;                                                        // Fine tuning amount
                COUNTS_PER_DEGREE       = (((2 * 3.1415 * ROBOT_TRACK) * COUNTS_PER_INCH) / 360) * WHEEL_TURN_FUDGE;
                loadPowerTableTankTread();                                                          //load the power table
                break;
            case "11231 2016 Custom": //2016 - 11231 Drivetrain
                COUNTS_PER_MOTOR_REV    = 1120;                                                     // eg: TETRIX = 1440 pulses, NeveRest 20 = 560 pulses, NeveRest 40 =  1120, NeveRest 60 = 1680 pulses
                DRIVE_GEAR_REDUCTION    = .667;                                                   // (.665) UP INCREASES THE DISTANCE This is < 1.0 if geared UP, Tilerunner is geared up
                WHEEL_DIAMETER_INCHES   = 4.0 ;                                                     // For figuring circumference
                WHEEL_ACTUAL_FUDGE      = 1;                                                        // Fine tuning amount
                COUNTS_PER_INCH         = ((COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415926535)) * WHEEL_ACTUAL_FUDGE ;
                ROBOT_TRACK             = 18;                                                     //  distance between centerline of rear wheels robot will pivot on rear wheel of omni on front, 16.5 track is 103.67 inches full circle
                COUNTS_PER_DEGREE       = ((2 * 3.1415926535 * ROBOT_TRACK) * COUNTS_PER_INCH) / 360;
                //loadPowerTableTileRunner();                                                         //load the power table
                break;
            default:  //default for competition TileRunner-2x40
                REVERSE_DIRECTION       = 1;
                COUNTS_PER_MOTOR_REV    = 1120 ;                                                     // eg: TETRIX = 1440 pulses, NeveRest 20 = 560 pulses, NeveRest 40 =  1120, NeveRest 60 = 1680 pulses
                DRIVE_GEAR_REDUCTION    = 1.28 ;                                                    // This is < 1.0 if geared UP, Tilerunner is geared up
                WHEEL_DIAMETER_INCHES   = 4.0 ;                                                     // For figuring circumference
                WHEEL_ACTUAL_FUDGE      = 1;                                                        // Fine tuning amount
                COUNTS_PER_INCH         = ((COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415)) * WHEEL_ACTUAL_FUDGE * REVERSE_DIRECTION ;
                ROBOT_TRACK             = 16.5;                                                     //  distance between centerline of rear wheels robot will pivot on rear wheel of omni on front, 16.5 track is 103.67 inches full circle
                COUNTS_PER_DEGREE       = ((2 * 3.1415 * ROBOT_TRACK) * COUNTS_PER_INCH) / 360;
                loadPowerTableTileRunner();                                                         //load the power table
                break;
        }

        if (debug >= 1)
        {
            fileLogger.writeEvent(TAG, "Team #            " +  teamNumber);
            fileLogger.writeEvent(TAG, "Alliance Colour   " +  allianceColor);
            fileLogger.writeEvent(TAG, "Alliance Position " +  alliancePosition);
            fileLogger.writeEvent(TAG, "Alliance Delay    " +  delay);
            fileLogger.writeEvent(TAG, "Alliance Beacons  " +  numBeacons);
            fileLogger.writeEvent(TAG, "Robot Config      " +  robotConfig);
            Log.d(TAG, "Team #            " +  teamNumber);
            Log.d(TAG, "Alliance Colour   " +  allianceColor);
            Log.d(TAG, "Alliance Position " +  alliancePosition);
            Log.d(TAG, "Alliance Delay    " +  delay);
            Log.d(TAG, "Alliance Beacons  " +  numBeacons);
            Log.d(TAG, "Robot Config      " +  robotConfig);
        }

        telemetry.addData("Init3       ",  "Loading Steps");
        telemetry.update();

        //need to load initial step of a delay based on user input
        loadSteps(delay + 2, "DEL" + (delay * 1000), false, false, 0,    0,    0,    0,    0,    0,  0);

        //load the sequence based on alliance colour and team
        switch (teamNumber) {
            case "5291":
                switch (allianceColor) {
                    case "Red":
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOff);
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsRedLeft5291();                                                               //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsRedRight5291();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Blue":
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOn);
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsBlueLeft5291();                                                              //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsBlueRight5291();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Test":
                        loadStaticSteps5291();                                                                  //load all the steps into the hashmaps
                        break;
                }
                break;

            case "11230":
                switch (allianceColor) {
                    case "Red":
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsRedLeft11230();                                                               //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsRedRight11230();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Blue":
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsBlueLeft11230();                                                              //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsBlueRight11230();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Test":
                        loadStaticSteps11230();                                                                  //load all the steps into the hashmaps
                        break;
                }
                break;

            case "11231":
                switch (allianceColor) {
                    case "Red":
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsRedLeft11231();                                                               //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsRedRight11231();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Blue":
                        switch (alliancePosition) {
                            case "Left":
                                loadStaticStepsBlueLeft11231();                                                              //load all the steps into the hashmaps
                                break;
                            case "Right":
                                loadStaticStepsBlueRight11231();                                                               //load all the steps into the hashmaps
                                break;
                        }
                        break;
                    case "Test":
                        loadStaticSteps11231();                                                                  //load all the steps into the hashmaps
                        break;
                }
                break;
        }


        //don't crash the program if the GRYO is faulty, just bypass it
        try {
            // get a reference to a Modern Robotics GyroSensor object.
            gyro = (ModernRoboticsI2cGyro)hardwareMap.gyroSensor.get("gyro");
            // calibrate the gyro, this takes a few seconds
            gyro.calibrate();
            telemetry.addData("Init4       ",  "Calibrating Gyro");
            telemetry.update();
        } catch (Exception e) {
            if (debug >= 1)
            {
                fileLogger.writeEvent(TAG, "Gyro Error " +  e.getMessage());
                Log.d(TAG, "Gyro Error " +  e.getMessage());
            }
            gyroError = true;
        }

        telemetry.addData("Init5       ",  "Line Sensors");
        telemetry.update();

        LineSensor1 = hardwareMap.get(AnalogInput.class, "linesensor1");
        LineSensor2 = hardwareMap.get(AnalogInput.class, "linesensor2");
        LineSensor3 = hardwareMap.get(AnalogInput.class, "linesensor3");
        /*
        * Initialize the drive system variables.
        * The init() method of the hardware class does all the work here
        */

        telemetry.addData("Init6       ",  "Base and Arm Motors");
        telemetry.update();

        robotDrive.init(hardwareMap);
        armDrive.init(hardwareMap);

        telemetry.addData("Init7       ",  "Range Sensors");
        telemetry.update();

        RANGE1 = hardwareMap.i2cDevice.get("range1");
        RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        RANGE1Reader.engage();
        RANGE2 = hardwareMap.i2cDevice.get("range2");
        RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        RANGE2Reader.engage();

        // get a reference to our ColorSensor object.
        try {
            telemetry.addData("Init8       ",  "Colour Sensor");
            telemetry.update();

            colorSensor = hardwareMap.colorSensor.get("sensorcolor");
        } catch (Exception e) {
            if (debug >= 1)
            {
                fileLogger.writeEvent(TAG, "colour Error " +  e.getMessage());
                Log.d(TAG, "colour Error " +  e.getMessage());
            }
            colourError = true;
        }

        telemetry.addData("Init9       ",  "Servos");
        telemetry.update();



        //config the servos
        servoBeaconRight = hardwareMap.servo.get("servobeaconright");
        servoBeaconLeft = hardwareMap.servo.get("servobeaconleft");
        servoBeaconRight.setDirection(Servo.Direction.REVERSE);
        servoLifterRight = hardwareMap.servo.get("servoliftright");
        servoLifterLeft = hardwareMap.servo.get("servoliftleft");
        servoLifterRight.setDirection(Servo.Direction.REVERSE);
        //lock the arms up
        moveServo(servoLifterRight, 135, SERVOLIFTRIGHT_MIN_RANGE, SERVOLIFTRIGHT_MAX_RANGE);
        moveServo(servoLifterLeft, 135, SERVOLIFTLEFT_MIN_RANGE, SERVOLIFTLEFT_MAX_RANGE);

        // Move the beacon pushers to home
        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);

        // Send telemetry message to signify robot waiting;
        telemetry.addData("Status", "Resetting Encoders");    //

        robotDrive.leftMotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robotDrive.leftMotor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robotDrive.rightMotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robotDrive.rightMotor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        mintCurrentStepState = stepState.STATE_INIT;
        mintCurrentTankTurnState = stepState.STATE_COMPLETE;
        mintCurrentDriveState = stepState.STATE_COMPLETE;
        mintCurrentDriveHeadingState = stepState.STATE_COMPLETE;
        mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
        mintCurrentRadiusTurnState = stepState.STATE_COMPLETE;
        mintCurrentDelayState = stepState.STATE_COMPLETE;
        mintCurrentVuforiaState = stepState.STATE_COMPLETE;
        mintCurrentVuforiaMoveState = stepState.STATE_COMPLETE;
        mintCurrentVuforiaTurnState = stepState.STATE_COMPLETE;
        mintCurrentAttackBeaconState = stepState.STATE_COMPLETE;
        mintCurrentBeaconColourState = stepState.STATE_COMPLETE;
        mint5291CurrentShootParticleState = stepState.STATE_COMPLETE;
        mint11231CurrentShootParticleState = stepState.STATE_COMPLETE;
        mintCurrentTankTurnGyroHeadingState = stepState.STATE_COMPLETE;
        mintCurrentTankTurnGyroBasicState = stepState.STATE_COMPLETE;
        mint5291CurrentLineFindState = stepState.STATE_COMPLETE;
        mint5291GyroTurnEncoderState = stepState.STATE_COMPLETE;

        mint5291LEDStatus = LEDState.STATE_TEAM;

        mblnNextStepLastPos = false;

        if (!gyroError) {
            while (!isStopRequested() && gyro.isCalibrating()) {
                sleep(50);
                idle();
            }
            telemetry.addData("Init10      ",  "Calibrated Gyro");
            telemetry.update();
        }

        if (debug >= 1)
        {
            fileLogger.writeEvent(TAG, "Init Complete");
            Log.d(TAG, "Init Complete");
        }

        //set up variable for our capturedimage
        Image rgb = null;

        //activate vuforia
        velocityVortex.activate();

        //show options on the driver station phone
        telemetry.addData("Init11      ",  "Complete");
        telemetry.addData("Team #     ",  teamNumber);
        telemetry.addData("Alliance   ",  allianceColor);
        telemetry.addData("Start Pos  ",  alliancePosition);
        telemetry.addData("Start Del  ",  delay);
        telemetry.addData("# Beacons  ",  numBeacons);
        telemetry.addData("Robot      ",  robotConfig);
        telemetry.update();

        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        //the main loop.  this is where the action happens
        while (opModeIsActive())
        {

            if (!mblnDisableVisionProcessing) {
                //start capturing frames for analysis
                if (readyToCapture) {
                    mintCaptureLoop ++;

                    VuforiaLocalizer.CloseableFrame frame = vuforia.getFrameQueue().take(); //takes the frame at the head of the queue
                    long numImages = frame.getNumImages();

                    for (int i = 0; i < numImages; i++) {
                        if (frame.getImage(i).getFormat() == PIXEL_FORMAT.RGB565) {
                            rgb = frame.getImage(i);
                            break;
                        }
                    }
                    /*rgb is now the Image object that we’ve used in the video*/
                    Bitmap bm = Bitmap.createBitmap(rgb.getWidth(), rgb.getHeight(), Bitmap.Config.RGB_565);
                    bm.copyPixelsFromBuffer(rgb.getPixels());

                    //put the image into a MAT for OpenCV
                    Mat tmp = new Mat(rgb.getWidth(), rgb.getHeight(), CvType.CV_8UC4);
                    Utils.bitmapToMat(bm, tmp);

                    //close the frame, prevents memory leaks and crashing
                    frame.close();

                    //analyse the beacons
                    //Constants.BeaconColours Colour = beaconColour.beaconAnalysisOCV(tmp, loop));
                    mColour = beaconColour.beaconAnalysisOCV(tmp, mintCaptureLoop);
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent("OPENCV","Returned " + mColour);
                        Log.d("OPENCV","Returned " + mColour);
                    }

                    telemetry.addData("Beacon ", mColour);
                }

                //use vuforia to get locations informatio
                for (VuforiaTrackable trackable : allTrackables) {
                    /**
                     * getUpdatedRobotLocation() will return null if no new information is available since
                     * the last time that call was made, or if the trackable is not currently visible.
                     * getRobotLocation() will return null if the trackable is not currently visible.
                     */
                    telemetry.addData(trackable.getName(), ((VuforiaTrackableDefaultListener) trackable.getListener()).isVisible() ? "Visible" : "Not Visible");    //

                    OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener) trackable.getListener()).getUpdatedRobotLocation();
                    if (robotLocationTransform != null) {
                        lastLocation = robotLocationTransform;
                    }
                }
                /**
                 * Provide feedback as to where the robot was last located (if we know).
                 */
                if (lastLocation != null) {
                    // Then you can extract the positions and angles using the getTranslation and getOrientation methods.
                    VectorF trans = lastLocation.getTranslation();
                    Orientation rot = Orientation.getOrientation(lastLocation, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES);
                    // Robot position is defined by the standard Matrix translation (x and y)
                    localisedRobotX = trans.get(0);
                    localisedRobotY = trans.get(1);

                    // Robot bearing (in Cartesian system) position is defined by the standard Matrix z rotation
                    localisedRobotBearing = rot.thirdAngle;
                    if (localisedRobotBearing < 0) {
                        localisedRobotBearing = 360 + localisedRobotBearing;
                    }

                    if (!mblnGryoResetVuforia) {
                        mintGyroFromVuforia = (int) localisedRobotBearing;
                        mblnGryoResetVuforia = true;
                    }

                    telemetry.addData("Pos X ", localisedRobotX);
                    telemetry.addData("Pos Y ", localisedRobotY);
                    telemetry.addData("Bear  ", localisedRobotBearing);
                    telemetry.addData("Pos   ", format(lastLocation));
                    localiseRobotPos = true;
                } else {
                    localiseRobotPos = false;
                    telemetry.addData("Pos   ", "Unknown");
                }
            }



            switch (mintCurrentStepState)
            {
                case STATE_INIT:
                {
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent(TAG, "mintCurrentStepState:- " + mintCurrentStepState + " mintCurrentStepState " + mintCurrentStepState);
                        fileLogger.writeEvent(TAG, "About to check if step exists " + mintCurrentStep);
                        Log.d(TAG, "mintCurrentStepState:- " + mintCurrentStepState + " mintCurrentStepState " + mintCurrentStepState);
                        Log.d(TAG, "About to check if step exists " + mintCurrentStep);
                    }
                    // get step from hashmap, send it to the initStep for decoding
                    if (autonomousSteps.containsKey(String.valueOf(mintCurrentStep)))
                    {
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG, "Step Exists TRUE " + mintCurrentStep + " about to get the values from the step");
                            Log.d(TAG, "Step Exists TRUE " + mintCurrentStep + " about to get the values from the step");
                        }
                        processingSteps = autonomousSteps.get(String.valueOf(mintCurrentStep));
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG, "Got the values for step " + mintCurrentStep + " about to decode");
                            Log.d(TAG, "Got the values for step " + mintCurrentStep + " about to decode");
                        }
                        //decode the step from hashmap
                        initStep(processingSteps);
                    }
                    else  //if no steps left in hashmap then complete
                    {
                        mintCurrentStepState = stepState.STATE_FINISHED;
                    }
                }
                break;
                case STATE_START:
                {

                }
                break;
                case STATE_RUNNING:
                {
                    DelayStep();
                    TankTurnStep();
                    TankTurnGyroHeading();
                    TankTurnGyroHeadingEncoder();
                    gyroTurnBasic();
                    PivotTurnStep();
                    RadiusTurnStep();
                    DriveStep();
                    DriveStepHeading();
                    VuforiaLocalise();
                    VuforiaMove();
                    VuforiaTurn();
                    AttackBeacon();
                    BeaconColour();
                    FlickerShooter5291();
                    FlickerShooter11231();
                    SFLineFind5291();

                    if ((mintCurrentDelayState == stepState.STATE_COMPLETE) &&
                            (mintCurrentBeaconColourState == stepState.STATE_COMPLETE) &&
                            (mintCurrentAttackBeaconState == stepState.STATE_COMPLETE) &&
                            (mintCurrentVuforiaTurnState == stepState.STATE_COMPLETE) &&
                            (mintCurrentVuforiaState == stepState.STATE_COMPLETE) &&
                            (mintCurrentVuforiaMoveState == stepState.STATE_COMPLETE)  &&
                            (mintCurrentDriveState == stepState.STATE_COMPLETE) &&
                            (mintCurrentDriveHeadingState == stepState.STATE_COMPLETE) &&
                            (mintCurrentPivotTurnState == stepState.STATE_COMPLETE) &&
                            (mintCurrentTankTurnState == stepState.STATE_COMPLETE) &&
                            (mint5291CurrentShootParticleState == stepState.STATE_COMPLETE) &&
                            (mint11231CurrentShootParticleState == stepState.STATE_COMPLETE) &&
                            (mint5291CurrentLineFindState == stepState.STATE_COMPLETE) &&
                            (mint5291GyroTurnEncoderState == stepState.STATE_COMPLETE) &&
                            (mintCurrentTankTurnGyroHeadingState == stepState.STATE_COMPLETE) &&
                            (mintCurrentTankTurnGyroBasicState == stepState.STATE_COMPLETE) &&
                            (mintCurrentRadiusTurnState == stepState.STATE_COMPLETE))
                    {
                        mintCurrentStepState = stepState.STATE_COMPLETE;
                    }

                    if (mblnParallel) {
                        //mark this step as complete and do next one, the current step should continue to run.  Not all steps are compatible with being run in parallel
                        // like drive steps, turns etc
                        // Drive forward and shoot
                        // Drive forward and detect beacon
                        // are examples of when parallel steps should be run
                        // errors will occur if other combinations are run
                        mintCurrentStepState = stepState.STATE_COMPLETE;
                    }

                }
                break;
                case STATE_PAUSE:
                {

                }
                break;
                case STATE_COMPLETE:
                {
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent(TAG, "Step Complete - Current Step:- " + mintCurrentStep);
                        Log.d(TAG, "Step Complete - Current Step:- " + mintCurrentStep);
                    }

                    //  Transition to a new state and next step.
                    mintCurrentStep++;
                    mintCurrentStepState = stepState.STATE_INIT;
                }
                break;
                case STATE_TIMEOUT:
                {
                    setDriveMotorPower(0);
                    //  Transition to a new state.
                    mintCurrentStepState = stepState.STATE_FINISHED;
                }
                break;
                case STATE_ERROR:
                {
                    telemetry.addData("STATE", "ERROR WAITING TO FINISH " + mintCurrentStep);
                }
                break;
                case STATE_FINISHED:
                {
                    setDriveMotorPower(0);

                    //deactivate vuforia
                    velocityVortex.deactivate();

                    telemetry.addData("STATE", "FINISHED " + mintCurrentStep);
                    if (debug >= 1)
                    {
                        if (fileLogger != null)
                        {
                            fileLogger.writeEvent(TAG, "Step FINISHED - FINISHED");
                            fileLogger.writeEvent(TAG, "Stopped");
                            Log.d(TAG, "FileLogger Stopped");
                            fileLogger.close();
                            fileLogger = null;
                        }
                    }
                }
                break;
                case STATE_ASTAR_PRE_INIT:
                {
                    mintCurrentAStarStep = 1;                                          //init the Step for AStar
                    //get start point
                    //get end point
                    int startX = (int)processingSteps.getmRobotParm1();
                    int startY = (int)processingSteps.getmRobotParm2();
                    int startZ = (int)processingSteps.getmRobotParm3();
                    int endX = (int)processingSteps.getmRobotParm4();
                    int endY = (int)processingSteps.getmRobotParm5();
                    int endDir = (int)processingSteps.getmRobotParm6();

                    //before using the path in the command lets check if we can localise
                    if (lastLocation != null) {
                        //lets get locations for AStar, direction is most important
                        //x and y position for Vuforia are in mm, AStar in Inches
                        //counter clockwise rotation (x,y) = (-x, y)
                        //origin is center of field
                        //Astar is top right so need to add in 6 feet to each value
                        startX = (int)(localisedRobotX/25.4) + 72;
                        startY = (int)(localisedRobotY/25.4) + 72;
                        //need to rotate the axis -90 degrees
                        startZ = (int)localisedRobotBearing;
                        if ((startZ > 357) && (startZ < 3))
                            startZ = 90;
                        else
                        if ((startZ > 267) && (startZ < 273))
                            startZ = 0;
                        else
                        if ((startZ > 177) && (startZ < 183))
                            startZ = 270;
                        else
                        if ((startZ > 87) && (startZ < 93))
                            startZ = 180;
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG, "AStar Init - Localised Values");
                            fileLogger.writeEvent(TAG, "AStar Init - localisedRobotX:        " + localisedRobotX);
                            fileLogger.writeEvent(TAG, "AStar Init - localisedRobotY:        " + localisedRobotY);
                            fileLogger.writeEvent(TAG, "AStar Init - localisedRobotBearing:  " + localisedRobotBearing);
                            fileLogger.writeEvent(TAG, "AStar Init - startX:                 " + startX);
                            fileLogger.writeEvent(TAG, "AStar Init - startY:                 " + startY);
                            fileLogger.writeEvent(TAG, "AStar Init - startZ:                 " + startZ);
                            Log.d(TAG, "AStar Init - Localised Values");
                            Log.d(TAG, "AStar Init - localisedRobotX:        " + localisedRobotX);
                            Log.d(TAG, "AStar Init - localisedRobotY:        " + localisedRobotY);
                            Log.d(TAG, "AStar Init - localisedRobotBearing:  " + localisedRobotBearing);
                            Log.d(TAG, "AStar Init - startX:                 " + startX);
                            Log.d(TAG, "AStar Init - startY:                 " + startY);
                            Log.d(TAG, "AStar Init - startZ:                 " + startZ);
                        }
                    }

                    //process path
                    pathValues = getPathValues.findPathAStar(startX, startY, startZ, endX, endY, endDir);  //for enhanced
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent(TAG, "AStar Path - length:                 " + pathValues.length);
                        Log.d(TAG, "AStar Path - length:                 " + pathValues.length);
                    }


                    String[][] mapComplete = new String[A0Star.FIELDWIDTH][A0Star.FIELDWIDTH];

                    //write path to logfile to verify path
                    for (int y = 0; y < a0Star.fieldLength; y++)
                    {
                        for (int x = 0; x < a0Star.fieldWidth; x++)
                        {
                            switch (allianceColor) {
                                case "Red":
                                    if (a0Star.walkableRed[y][x]) {
                                        mapComplete[y][x] = "1";
                                        if ((x == startX) && (y == startY))
                                            mapComplete[y][x] = "S";
                                        else if ((x == endX) && (y == endY))
                                            mapComplete[y][x] = "E";
                                    } else {
                                        mapComplete[y][x] = "0";
                                    }
                                    break;

                                case "Blue":
                                    if (a0Star.walkableBlue[y][x]) {
                                        mapComplete[y][x] = "1";
                                        if ((x == startX) && (y == startY))
                                            mapComplete[y][x] = "S";
                                        else if ((x == endX) && (y == endY))
                                            mapComplete[y][x] = "E";
                                    } else {
                                        if ((x == startX) && (y == startY)) {
                                            mapComplete[y][x] = "1";
                                        } else {
                                            mapComplete[y][x] = "0";
                                        }
                                    }
                                    break;

                                default:
                                    if (a0Star.walkable[y][x]) {
                                        mapComplete[y][x] = "1";
                                        if ((x == startX) && (y == startY))
                                            mapComplete[y][x] = "S";
                                        else if ((x == endX) && (y == endY))
                                            mapComplete[y][x] = "E";
                                    }
                                    break;
                            }
                        }
                    }

                    //plot out path..
                    for (int i = 0; i < pathValues.length; i++)
                    {
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG,"Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4 );
                            Log.d(TAG,"Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4 );
                        }
                        if (((int)pathValues[i].val1 == 0) && ((int)pathValues[i].val3 == 0) && ((int)pathValues[i].val2 == 0) && ((int)pathValues[i].val4 == 0))
                            break;
                        mapComplete[(int)pathValues[i].val3][(int)pathValues[i].val2] = "P";
                        if ((pathValues[i].val2 == startX) && (pathValues[i].val3 == startY))
                        {
                            mapComplete[(int) pathValues[i].val3][(int) pathValues[i].val2] = "S";
                        }
                    }
                    mapComplete[endY][endX] = "E";
                    fieldOutput ="";
                    for (int y = 0; y < a0Star.fieldLength; y++)
                    {
                        for (int x = 0; x < a0Star.fieldWidth; x++)
                        {
                            fieldOutput = "" + fieldOutput + mapComplete[y][x];
                        }
                        if (debug >= 2)
                        {
                            fileLogger.writeEvent(TAG, fieldOutput);
                            Log.d(TAG, fieldOutput);
                        }
                        fieldOutput = "";
                    }

                    //load path in Hashmap
                    boolean dirChanged;
                    boolean processingAStarSteps = true;
                    int startSegment = 1;
                    int startStraightSection = 0;
                    int numberOfMoves = 0;
                    int key = 0;
                    int lastDirection = 0;
                    int lasti =0;
                    String strAngleChange = "RT00";
                    boolean endOfAStarSequenceFound = false;

                    while (processingAStarSteps)
                    {
                        numberOfMoves = 0;
                        for (int i = startSegment; i < pathValues.length; i++)
                        {
                            numberOfMoves ++;
                            if (debug >= 2)
                            {
                                fileLogger.writeEvent(TAG,"Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4 );
                                Log.d(TAG,"Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4 );
                            }
                            if (((int)pathValues[i].val1 == 0) && ((int)pathValues[i].val2 == 0) && ((int)pathValues[i].val3 == 0))
                            {
                                if (debug >= 2)
                                {
                                    fileLogger.writeEvent(TAG,"End Detected" );
                                    Log.d(TAG,"End Detected" );
                                }
                                //end of the sequence,
                                lastDirection = (int)pathValues[i-1].val4;
                                processingAStarSteps = false;
                                lasti = i;
                            }
                            //need to check if the first step is in a different direction that the start
                            if (i == 1) {
                                if (startZ != pathValues[i].val4) {  //need to turn
                                    strAngleChange = getAngle(startZ, (int) pathValues[i].val4);
                                    if (debug >= 2) {
                                        fileLogger.writeEvent(TAG, "First Step Need to turn Robot " + strAngleChange + " Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4);
                                        fileLogger.writeEvent(TAG, "Adding Command (" + key + ", 10, " + strAngleChange + ", 0, 0, 0, 0, 0, 0, 1, false) ");
                                        Log.d(TAG, "First Step Need to turn Robot " + strAngleChange + " Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4);
                                        Log.d(TAG, "Adding Command (" + key + ", 10, " + strAngleChange + ", 0, 0, 0, 0, 0, 0, 1, false) ");
                                    }
                                    autonomousStepsAStar.put(String.valueOf(key), new LibraryStateSegAuto(key, 10, strAngleChange, false, false, 0, 0, 0, 0, 0, 0, 1, false));
                                    key++;
                                    dirChanged = true;
                                } else {
                                    dirChanged = false;    //no change in direction
                                }
                            }
                            else  //work out the sequence not the first step
                            {
                                if (pathValues[i-1].val4 != pathValues[i].val4) {  //need to turn
                                    strAngleChange = getAngle((int)pathValues[i-1].val4, (int)pathValues[i].val4);
                                    dirChanged = true;
                                }
                                else
                                {
                                    dirChanged = false;    //no change in direction
                                }
                            }
                            if ((dirChanged) || (!processingAStarSteps))  //found end of segment
                            {
                                int AStarPathAngle;
                                if (i == 1)
                                {
                                    AStarPathAngle = startZ;
                                }
                                else
                                {
                                    AStarPathAngle = (int)pathValues[i-1].val4;
                                }
                                switch (AStarPathAngle)
                                {
                                    case 0:
                                    case 90:
                                    case 180:
                                    case 270:
                                        if (debug >= 2)
                                        {
                                            fileLogger.writeEvent(TAG,"Heading on a Straight line " + (numberOfMoves) + " Path");
                                            fileLogger.writeEvent(TAG,"Adding Command (" + key +", 10, " + "FW" + (numberOfMoves) + ", false, false, 0, 0, 0, 0, 0, 0, 0.8, false) ");
                                        }
                                        autonomousStepsAStar.put(String.valueOf(key), new LibraryStateSegAuto (key, 10, "FW" + numberOfMoves , false, false, 0, 0, 0, 0, 0, 0, 0.8, false));
                                        numberOfMoves = 0;
                                        key++;
                                        break;
                                    case 45:
                                    case 135:
                                    case 225:
                                    case 315:
                                        if (debug >= 2)
                                        {
                                            fileLogger.writeEvent(TAG,"Heading on a Straight line " + (int) ((numberOfMoves) * 1.4142) + " Path");
                                            fileLogger.writeEvent(TAG, "Adding Command (" + key + ", 10, " + "FW" + (int) ((numberOfMoves) * 1.4142) + ", false, false, 0, 0, 0, 0, 0, 0, .8, false) ");
                                        }
                                        autonomousStepsAStar.put(String.valueOf(key), new LibraryStateSegAuto (key, 10, "FW" + (int)(numberOfMoves * 1.4142 ) , false, false, 0,    0,    0,    0,    0,    0,    1,    false));
                                        numberOfMoves = 0;
                                        key++;
                                        break;
                                }
                                if (debug >= 2)
                                {
                                    fileLogger.writeEvent(TAG,"Need to turn Robot " + strAngleChange + " Path " + pathValues[i].val1 + " " + pathValues[i].val2 + " " + pathValues[i].val3 + " Dir:= " + pathValues[i].val4 );
                                    fileLogger.writeEvent(TAG,"Adding Command (" + key +", 10, "+ strAngleChange + ", 0, 0, 0, 0, 0, 0, 1, false) ");
                                }
                                autonomousStepsAStar.put(String.valueOf(key), new LibraryStateSegAuto (key, 10, strAngleChange, false, false, 0, 0, 0, 0, 0, 0, 0.4, false));
                                key++;
                            }
                            if (!processingAStarSteps)
                                break;

                        }
                        //need to work out the direction we are facing and the required direction
                        if ((lastDirection != endDir) && (!processingAStarSteps)) {
                            if (debug >= 2)
                            {
                                fileLogger.writeEvent(TAG,"Sraight Moves Robot End Of Sequence - Need to Trun Robot");
                            }
                            strAngleChange = getAngle((int)pathValues[lasti - 1].val4, endDir);
                            fileLogger.writeEvent(TAG,"Adding Command (" + key +", 10, " + strAngleChange + ", 0, 0, 0, 0, 0, 0, 1, false) ");
                            autonomousStepsAStar.put(String.valueOf(key), new LibraryStateSegAuto (key, 10,  strAngleChange, false, false, 0, 0, 0, 0, 0, 0, 0.4, false));
                            key++;
                        }
                    }
                    endOfAStarSequenceFound = false;
                    mintCurrentStepState = stepState.STATE_ASTAR_INIT;
                }
                break;
                case STATE_ASTAR_INIT:
                {
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent(TAG, "About to check if step exists " + mintCurrentAStarStep);
                    }
                    // get step from hashmap, send it to the initStep for decoding
                    if (autonomousStepsAStar.containsKey(String.valueOf(mintCurrentAStarStep)))
                    {
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG, "Step Exists TRUE " + mintCurrentAStarStep + " about to get the values from the step");
                        }
                        processingSteps = autonomousStepsAStar.get(String.valueOf(mintCurrentAStarStep));      //read the step from the hashmap
                        autonomousStepsAStar.remove(String.valueOf(mintCurrentAStarStep));                     //remove the step from the hashmap
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent(TAG, "Got the values for step " + mintCurrentAStarStep + " about to decode and removed them");
                        }

                        //decode the step from hashmap
                        initAStarStep(processingSteps);
                    }
                    else  //if no steps left in hashmap then complete
                    {
                        mintCurrentStepState = stepState.STATE_ASTAR_COMPLETE;
                    }
                }
                break;
                case STATE_ASTAR_RUNNING:
                {
                    //move robot according AStar hashmap
                    TankTurnStep();
                    PivotTurnStep();
                    RadiusTurnStep();
                    DriveStep();
                    if ((mintCurrentDriveState == stepState.STATE_COMPLETE) &&
                            (mintCurrentPivotTurnState == stepState.STATE_COMPLETE) &&
                            (mintCurrentTankTurnState == stepState.STATE_COMPLETE) &&
                            (mintCurrentRadiusTurnState == stepState.STATE_COMPLETE))
                    {
                        //increment ASTar Steps Counter
                        mintCurrentAStarStep++;
                        mintCurrentStepState = stepState.STATE_ASTAR_INIT;
                    }

                }
                break;
                case STATE_ASTAR_ERROR:
                {
                    //do something on error
                }
                break;
                case STATE_ASTAR_COMPLETE:
                {
                    //empty hashmap ready for next AStar processing.
                    //clear AStar step counter ready for next AStar process
                    mintCurrentAStarStep = 0;

                    //when complete, keep processing normal step
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent(TAG, "A* Path Completed:- " + mintCurrentStep);
                        Log.d(TAG, "A* Path Completed:- " + mintCurrentStep);
                    }

                    //  Transition to a new state and next step.
                    mintCurrentStep++;
                    mintCurrentStepState = stepState.STATE_INIT;
                }
                break;
            }

            //check timeout vale
            if ((mStateTime.seconds() > mdblStepTimeout  ) && ((mintCurrentStepState != stepState.STATE_ERROR) && (mintCurrentStepState != stepState.STATE_FINISHED)))
            {
                if (debug >= 1)
                {
                    fileLogger.writeEvent(TAG, "Timeout:- ");
                    Log.d(TAG, "Timeout:- ");
                }
                //  Transition to a new state.
                mintCurrentStepState = stepState.STATE_TIMEOUT;
            }

            //process LED status
            //ERROR - FLASH RED 3 TIMES
            switch (mint5291LEDStatus) {
                case STATE_TEAM:        //FLASH Alliance Colour
                    if (allianceColor.equals("Red"))
                        LedState(LedOff, LedOn, LedOff, LedOff, LedOn, LedOff);
                    else if (allianceColor.equals("Blue"))
                        LedState(LedOff, LedOff, LedOn, LedOff, LedOff, LedOn);
                    else
                        LedState(LedOn, LedOn, LedOn, LedOn, LedOn, LedOn);
                    break;
                case STATE_ERROR:       //Flash RED 3 times Rapidly
                    if ((!mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOff + 250))) {
                        mdblLastOn = mStateTime.milliseconds();
                        mblnLEDON = true;
                        LedState(LedOff, LedOn, LedOff, LedOff, LedOn, LedOff);
                    } else  if ((mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOn + 750))) {
                        mdblLastOff = mStateTime.milliseconds();
                        mblnLEDON = false;
                        LedState(LedOff, LedOff, LedOff, LedOff, LedOff, LedOff);
                    }
                    break;
                case STATE_SUCCESS:       //Flash GREEN 3 times Rapidly
                    if ((!mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOff + 250))) {
                        mdblLastOn = mStateTime.milliseconds();
                        mblnLEDON = true;
                        LedState(LedOn, LedOff, LedOff, LedOn, LedOff, LedOff);
                    } else  if ((mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOn + 250))) {
                        mdblLastOff = mStateTime.milliseconds();
                        mblnLEDON = false;
                        LedState(LedOff, LedOff, LedOff, LedOff, LedOff, LedOff);
                        mintCounts ++;
                    }
                    if (mintCounts >= 5) {
                        mintCounts = 0;
                        mint5291LEDStatus = LEDState.STATE_TEAM;
                    }
                    break;
                case STATE_BEACON:       //

                    if ((!mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOff + 500))) {
                        mdblLastOn = mStateTime.milliseconds();
                        mblnLEDON = true;
                        if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                            LedState(LedOff, LedOff, LedOn, LedOff, LedOff, LedOff);
                        } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                            LedState(LedOff, LedOn, LedOff, LedOff, LedOff, LedOff);
                        } else if (mColour == Constants.BeaconColours.BEACON_BLUE) {
                            LedState(LedOn, LedOff, LedOff, LedOff, LedOff, LedOff);
                        } else if (mColour == Constants.BeaconColours.BEACON_BLUE) {
                            LedState(LedOff, LedOn, LedOff, LedOff, LedOff, LedOff);
                        }
                    } else  if ((mblnLEDON) && (mStateTime.milliseconds() > (mdblLastOn + 500))) {
                        mdblLastOff = mStateTime.milliseconds();
                        mblnLEDON = false;
                        if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                            LedState(LedOff, LedOff, LedOff, LedOff, LedOn, LedOff);
                        } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                            LedState(LedOff, LedOff, LedOff, LedOff, LedOff, LedOn);
                        } else if (mColour == Constants.BeaconColours.BEACON_BLUE) {
                            LedState(LedOff, LedOff, LedOff, LedOn, LedOff, LedOff);
                        } else if (mColour == Constants.BeaconColours.BEACON_BLUE) {
                            LedState(LedOff, LedOff, LedOff, LedOff, LedOn, LedOff);
                        }
                        mintCounts ++;
                    }
                    if (mintCounts >= 10) {
                        mintCounts = 0;
                        mint5291LEDStatus = LEDState.STATE_TEAM;
                    }
                    break;
                case STATE_FINISHED:      //Solid Green
                    LedState(LedOn, LedOff, LedOff, LedOn, LedOff, LedOff);
                    break;

            }
            telemetry.update();
        }

        //opmode not active anymore

    }

    //--------------------------------------------------------------------------
    // User Defined Utility functions here....
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //  Initialise the state.
    //--------------------------------------------------------------------------
    private void initStep (LibraryStateSegAuto mStateSegAuto) {
        mdblStepDistance = 0;

        if (debug >= 3)
        {
            fileLogger.writeEvent(TAG, "Starting to Decode Step ");
            Log.d(TAG, "Starting to Decode Step ");
        }

        // Reset the state time, and then change to next state.
        mblnBaseStepComplete = false;
        mStateTime.reset();

        mdblStepTimeout = mStateSegAuto.getmRobotTimeOut();
        mdblStepSpeed = mStateSegAuto.getmRobotSpeed();
        mdblRobotCommand = mStateSegAuto.getmRobotCommand();
        mdblRobotParm1 = mStateSegAuto.getmRobotParm1();
        mdblRobotParm2 = mStateSegAuto.getmRobotParm2();
        mdblRobotParm3 = mStateSegAuto.getmRobotParm3();
        mdblRobotParm4 = mStateSegAuto.getmRobotParm4();
        mdblRobotParm5 = mStateSegAuto.getmRobotParm5();
        mdblRobotParm6 = mStateSegAuto.getmRobotParm6();
        mblnParallel =  mStateSegAuto.getmRobotParallel();
        mblnRobotLastPos = mStateSegAuto.getmRobotLastPos();
        mdblRobotStepComplete = mStateSegAuto.getmRobotStepComplete();

        if (debug >= 3) {
            fileLogger.writeEvent("initStep()", "mdblRobotCommand.substring(0, 3)    :- " + mdblRobotCommand.substring(0, 3));
            fileLogger.writeEvent("initStep()", "mdblRobotCommand.substring(3)       :- " + mdblRobotCommand.substring(3));
        }

        mintCurrentStepState = stepState.STATE_RUNNING;

        switch (mdblRobotCommand.substring(0, 3))
        {
            case "DEL":
                mintCurrentDelayState = stepState.STATE_INIT;
                break;
            case "GTH":
                mintCurrentTankTurnGyroHeadingState = stepState.STATE_INIT;
                break;
            case "GTB":  // Shoot the Particle balls
                mintCurrentTankTurnGyroBasicState = stepState.STATE_INIT;
                break;
            case "LTE":
                mintCurrentTankTurnState = stepState.STATE_INIT;
                break;
            case "RTE":
                mintCurrentTankTurnState = stepState.STATE_INIT;
                break;
            case "LPE":
                mintCurrentPivotTurnState = stepState.STATE_INIT;
                break;
            case "RPE":
                mintCurrentPivotTurnState = stepState.STATE_INIT;
                break;
            case "LRE":  // Left turn with a Radius in Parm 1
                mintCurrentRadiusTurnState = stepState.STATE_INIT;
                break;
            case "RRE":  // Right turn with a Radius in Parm 1
                mintCurrentRadiusTurnState = stepState.STATE_INIT;
                break;
            case "FWE":  // Drive forward a distance in inches and power setting
                //mintCurrentDriveState = stepState.STATE_INIT;
                mintCurrentDriveHeadingState = stepState.STATE_INIT;
                break;
            case "ASE":  // Plot a course using A* algorithm, accuracy in Parm 1
                mintCurrentStepState = stepState.STATE_ASTAR_PRE_INIT;
                break;
            case "VFL":  // Position the robot using vuforia parameters ready fro AStar  RObot should postion pointing to Red wall and Blue wall where targets are located
                mintCurrentVuforiaState = stepState.STATE_INIT;
                break;
            case "VME":  // Move the robot using localisation from the targets
                mintCurrentVuforiaMoveState = stepState.STATE_INIT;
                break;
            case "VTE":  // Turn the Robot using information from Vuforia and Pythag
                mintCurrentVuforiaTurnState = stepState.STATE_INIT;
                break;
            case "BCL":  // Get the beacon colour and move the robot to press the button
                mintCurrentBeaconColourState = stepState.STATE_INIT;
                break;
            case "ATB":  // Press the beacon button robot to press the button
                mintCurrentAttackBeaconState = stepState.STATE_INIT;
                break;
            case "ST1":  // Shoot the Particle balls
                mint5291CurrentShootParticleState = stepState.STATE_INIT;
                break;
            case "ST2":  // Shoot the Particle balls
                mint11231CurrentShootParticleState = stepState.STATE_INIT;
                break;
            case "SF1":  // Special Function, 5291 Move forward until line is found
                mint5291CurrentLineFindState = stepState.STATE_INIT;
                break;
            case "GTE":  // Special Function, 5291 Move forward until line is found
                mint5291GyroTurnEncoderState = stepState.STATE_INIT;
                break;
            case "FNC":  //  Run a special Function with Parms

                break;
        }

        if (debug >= 2) {
            fileLogger.writeEvent("initStep()", "Current Step          :- " + mintCurrentStep);
            fileLogger.writeEvent("initStep()", "mdblStepTimeout       :- " + mdblStepTimeout);
            fileLogger.writeEvent("initStep()", "mdblStepSpeed         :- " + mdblStepSpeed);
            fileLogger.writeEvent("initStep()", "mdblRobotCommand      :- " + mdblRobotCommand);
            fileLogger.writeEvent("initStep()", "mblnParallel          :- " + mblnParallel);
            fileLogger.writeEvent("initStep()", "mblnRobotLastPos      :- " + mblnRobotLastPos);
            fileLogger.writeEvent("initStep()", "mdblRobotParm1        :- " + mdblRobotParm1);
            fileLogger.writeEvent("initStep()", "mdblRobotParm2        :- " + mdblRobotParm2);
            fileLogger.writeEvent("initStep()", "mdblRobotParm3        :- " + mdblRobotParm3);
            fileLogger.writeEvent("initStep()", "mdblRobotParm4        :- " + mdblRobotParm4);
            fileLogger.writeEvent("initStep()", "mdblRobotParm5        :- " + mdblRobotParm5);
            fileLogger.writeEvent("initStep()", "mdblRobotParm6        :- " + mdblRobotParm6);
            fileLogger.writeEvent("initStep()", "mdblStepDistance      :- " + mdblStepDistance);
            fileLogger.writeEvent("initStep()", "mdblStepTurnL         :- " + mdblStepTurnL);
            fileLogger.writeEvent("initStep()", "mdblStepTurnR         :- " + mdblStepTurnR);
            fileLogger.writeEvent("initStep()", "mdblRobotStepComplete :- " + mdblRobotStepComplete);
        }
    }

    private void initAStarStep (LibraryStateSegAuto mStateSegAuto) {
        mdblStepDistance = 0;

        if (debug >= 3)
        {
            fileLogger.writeEvent(TAG, "Starting to Decode AStar Step ");
        }

        // Reset the state time, and then change to next state.
        mblnBaseStepComplete = false;
        mStateTime.reset();

        mdblStepTimeout = mStateSegAuto.getmRobotTimeOut();
        mdblStepSpeed = mStateSegAuto.getmRobotSpeed();
        mdblRobotCommand = mStateSegAuto.getmRobotCommand();
        mdblRobotParm1 = mStateSegAuto.getmRobotParm1();
        mdblRobotParm2 = mStateSegAuto.getmRobotParm2();
        mdblRobotParm3 = mStateSegAuto.getmRobotParm3();
        mdblRobotParm4 = mStateSegAuto.getmRobotParm4();
        mdblRobotParm5 = mStateSegAuto.getmRobotParm5();
        mdblRobotParm6 = mStateSegAuto.getmRobotParm6();
        mdblRobotStepComplete = mStateSegAuto.getmRobotStepComplete();

        mintCurrentStepState = stepState.STATE_ASTAR_RUNNING;

        switch (mdblRobotCommand.substring(0, 3))
        {
            case "DEL":
                mintCurrentDelayState = stepState.STATE_INIT;
                break;
            case "LTE":
                mintCurrentTankTurnState = stepState.STATE_INIT;
                break;
            case "RTE":
                mintCurrentTankTurnState = stepState.STATE_INIT;
                break;
            case "LPE":
                mintCurrentPivotTurnState = stepState.STATE_INIT;
                break;
            case "RPE":
                mintCurrentPivotTurnState = stepState.STATE_INIT;
                break;
            case "LRE":  // Left turn with a Radius in Parm 1
                mintCurrentRadiusTurnState = stepState.STATE_INIT;
                break;
            case "RRE":  // Right turn with a Radius in Parm 1
                mintCurrentRadiusTurnState = stepState.STATE_INIT;
                break;
            case "FWE":  // Drive forward a distance in inches and power setting
                mintCurrentDriveState = stepState.STATE_INIT;
                break;
            case "ASE":  // Plot a course using A* algorithm, accuracy in Parm 1
                mintCurrentStepState = stepState.STATE_ASTAR_PRE_INIT;
                break;
            case "FNC":  //  Run a special Function with Parms

                break;
        }

        if (debug >= 2) {
            fileLogger.writeEvent("initAStarStep()", "Current Step          :- " + mintCurrentStep);
            fileLogger.writeEvent("initAStarStep()", "mdblStepTimeout       :- " + mdblStepTimeout);
            fileLogger.writeEvent("initAStarStep()", "mdblStepSpeed         :- " + mdblStepSpeed);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotCommand      :- " + mdblRobotCommand);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm1        :- " + mdblRobotParm1);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm2        :- " + mdblRobotParm2);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm3        :- " + mdblRobotParm3);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm4        :- " + mdblRobotParm4);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm5        :- " + mdblRobotParm5);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotParm6        :- " + mdblRobotParm6);
            fileLogger.writeEvent("initAStarStep()", "mdblStepDistance      :- " + mdblStepDistance);
            fileLogger.writeEvent("initAStarStep()", "mdblStepTurnL         :- " + mdblStepTurnL);
            fileLogger.writeEvent("initAStarStep()", "mdblStepTurnR         :- " + mdblStepTurnR);
            fileLogger.writeEvent("initAStarStep()", "mdblRobotStepComplete :- " + mdblRobotStepComplete);
        }
    }

    private void DriveStep()
    {
        double dblStepSpeedTemp;
        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        double dblDistanceToEnd;
        double dblDistanceFromStartLeft1;
        double dblDistanceFromStartLeft2;
        double dblDistanceFromStartRight1;
        double dblDistanceFromStartRight2;
        double dblDistanceFromStart;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;

        switch (mintCurrentDriveState)
        {
            case STATE_INIT:
            {
                // set motor controller to mode
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mdblStepDistance = 0;
                mblnDisableVisionProcessing = true;  //disable vision processing

                switch (mdblRobotCommand.substring(0, 3)) {
                    case "FWE":  // Drive a distance in inches and power setting
                        mdblStepDistance = Double.parseDouble(mdblRobotCommand.substring(3));
                        break;
                }

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveStep()", "mdblStepDistance   :- " + mdblStepDistance);
                }
                // Determine new target position

                if (mblnNextStepLastPos) {
                    mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                    mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                    mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                    mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                } else {
                    mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                    mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                    mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                    mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                }
                mblnNextStepLastPos = false;

                mintStepLeftTarget1 = mintStartPositionLeft1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepLeftTarget2 = mintStartPositionLeft2 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget1 = mintStartPositionRight1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget2 = mintStartPositionRight2 + (int) (mdblStepDistance * COUNTS_PER_INCH);

                //store the encoder positions so next step can calculate destination
                mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                // pass target position to motor controller
                robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);
                robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveStep()", "mStepLeftTarget1 :- " + mintStepLeftTarget1 +  " mStepLeftTarget2 :- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("runningDriveStep()", "mStepRightTarget1:- " + mintStepRightTarget1 + " mStepRightTarget2:- " + mintStepRightTarget2);
                }

                if (!(robotDrive.leftMotor1.getMode().equals(DcMotor.RunMode.RUN_TO_POSITION)));
                // set motor controller to mode, Turn On RUN_TO_POSITION
                {
                    robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                }
                mintCurrentDriveState = stepState.STATE_RUNNING;
            }
            break;
            case STATE_RUNNING:
            {
                dblStepSpeedTemp = mdblStepSpeed;

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                // ramp up speed - need to write function to ramp up speed
                dblDistanceFromStartLeft1 = Math.abs(mintStartPositionLeft1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartLeft2 = Math.abs(mintStartPositionLeft2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartRight1 = Math.abs(mintStartPositionRight1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartRight2 = Math.abs(mintStartPositionRight2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                //if moving ramp up
                dblDistanceFromStart = (dblDistanceFromStartLeft1 + dblDistanceFromStartRight1 + dblDistanceFromStartLeft2 + dblDistanceFromStartRight2) / 4;

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                //if getting close ramp down speed
                dblDistanceToEnd = (dblDistanceToEndLeft1 + dblDistanceToEndRight1 + dblDistanceToEndLeft2 + dblDistanceToEndRight2) / 4;
/*
                if ((dblDistanceFromStart <= 0.5 ) || (dblDistanceToEnd <= 0.5 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(0.5)));
                }
                else if ((dblDistanceFromStart <= 1 ) || (dblDistanceToEnd <= 1 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(1)));
                }
                else if ((dblDistanceFromStart <= 2 ) || (dblDistanceToEnd <= 2 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(2)));
                }
                else if ((dblDistanceFromStart <= 4 ) || (dblDistanceToEnd <= 4 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(4)));
                }
                else if ((dblDistanceFromStart <= 6 ) || (dblDistanceToEnd <= 6 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(6)));
                }
                else if ((dblDistanceFromStart <= 8 ) || (dblDistanceToEnd <= 8 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(8)));
                }
                else if ((dblDistanceFromStart <= 10 ) || (dblDistanceToEnd <= 10 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(10)));
                }
                else if ((dblDistanceFromStart <= 12 ) || (dblDistanceToEnd <= 12 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(12)));
                }
                else if ((dblDistanceFromStart <= 15 ) || (dblDistanceToEnd <= 15 ))
                {
                    dblStepSpeedTemp = Double.valueOf(powerTable.get(String.valueOf(15)));
                }

                if (mStepSpeed < dblStepSpeedTemp)
                    dblStepSpeedTemp = mStepSpeed;*/

                if (mblnRobotLastPos) {
                    if (dblDistanceToEnd <= 3.0) {
                        if (debug >= 2)
                        {
                            fileLogger.writeEvent("runningDriveStep()", "mblnRobotLastPos Complete         ");
                        }
                        mblnNextStepLastPos = true;
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentDriveState = stepState.STATE_COMPLETE;
                    }
                }

                //if within error margin stop
                if (robotDrive.leftMotor1.isBusy() && robotDrive.rightMotor1.isBusy())
                {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("runningDriveStep()", "Encoder counts per inch = " + COUNTS_PER_INCH + " dblDistanceFromStart " + dblDistanceFromStart + " dblDistanceToEnd " + dblDistanceToEnd + " Power Level " + dblStepSpeedTemp + " Running to target  L1, L2, R1, R2  " + mintStepLeftTarget1 + ", " + mintStepLeftTarget2 + ", " + mintStepRightTarget1 + ",  " + mintStepRightTarget2 + ", " + " Running at position L1 " + intLeft1MotorEncoderPosition + " L2 " + intLeft2MotorEncoderPosition + " R1 " + intRight1MotorEncoderPosition + " R2 " + intRight2MotorEncoderPosition);
                    }
                    telemetry.addData("Path1", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                    telemetry.addData("Path2", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intRight1MotorEncoderPosition);
                    telemetry.addData("Path3", "Running at %7d :%7d", intLeft2MotorEncoderPosition, intRight2MotorEncoderPosition);
                    // set power on motor controller to start moving
                    setDriveMotorPower(Math.abs(dblStepSpeedTemp));
                }
                else
                {
                    // Stop all motion;
                    setDriveMotorPower(0);
                    mblnBaseStepComplete = true;
                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("runningDriveStep()", "Complete         ");
                    }
                    mblnDisableVisionProcessing = false;  //enable vision processing
                    mintCurrentDriveState = stepState.STATE_COMPLETE;
                }


            }
            break;
        }
    }

    private void DriveStepHeading()
    {
        double dblStepSpeedTemp;
        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        double dblDistanceToEnd;
        double dblDistanceFromStartLeft1;
        double dblDistanceFromStartLeft2;
        double dblDistanceFromStartRight1;
        double dblDistanceFromStartRight2;
        double dblDistanceFromStart;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;
        double  dblMaxSpeed;
        double  dblError;
        double  dblSteer;
        double  dblLeftSpeed;
        double  dblRightSpeed;

        switch (mintCurrentDriveHeadingState)
        {
            case STATE_INIT:
            {
                // set motor controller to mode
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mblnDisableVisionProcessing = true;  //disable vision processing
                mdblStepDistance = Double.parseDouble(mdblRobotCommand.substring(3));

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveHeadingStep()", "mdblStepDistance   :- " + mdblStepDistance);
                }
                // Determine new target position

                if (mblnNextStepLastPos) {
                    mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                    mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                    mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                    mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                } else {
                    mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                    mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                    mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                    mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                }
                mblnNextStepLastPos = false;

                mintStepLeftTarget1 = mintStartPositionLeft1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepLeftTarget2 = mintStartPositionLeft2 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget1 = mintStartPositionRight1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget2 = mintStartPositionRight2 + (int) (mdblStepDistance * COUNTS_PER_INCH);

                //store the encoder positions so next step can calculate destination
                mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                // pass target position to motor controller
                robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);
                robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveHeadingStep()", "mStepLeftTarget1 :- " + mintStepLeftTarget1 +  " mStepLeftTarget2 :- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("runningDriveHeadingStep()", "mStepRightTarget1:- " + mintStepRightTarget1 + " mStepRightTarget2:- " + mintStepRightTarget2);
                }

                if (!(robotDrive.leftMotor1.getMode().equals(DcMotor.RunMode.RUN_TO_POSITION)));
                // set motor controller to mode, Turn On RUN_TO_POSITION
                {
                    robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                }

                mintCurrentDriveHeadingState = stepState.STATE_RUNNING;
                setDriveMotorPower(Math.abs(mdblStepSpeed));
            }
            break;
            case STATE_RUNNING:
            {
                dblStepSpeedTemp = mdblStepSpeed;

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                // ramp up speed - need to write function to ramp up speed
                dblDistanceFromStartLeft1 = Math.abs(mintStartPositionLeft1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartLeft2 = Math.abs(mintStartPositionLeft2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartRight1 = Math.abs(mintStartPositionRight1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceFromStartRight2 = Math.abs(mintStartPositionRight2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                //if moving ramp up
                dblDistanceFromStart = (dblDistanceFromStartLeft1 + dblDistanceFromStartRight1 + dblDistanceFromStartLeft2 + dblDistanceFromStartRight2) / 4;

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                //if getting close ramp down speed
                dblDistanceToEnd = (dblDistanceToEndLeft1 + dblDistanceToEndRight1 + dblDistanceToEndLeft2 + dblDistanceToEndRight2) / 4;

                if ((mdblRobotParm1 == 1) || (mdblRobotParm4 == 1)) {
                    dblLeftSpeed = dblStepSpeedTemp;
                    dblRightSpeed = dblStepSpeedTemp;
                    //use Gyro to run heading
                    // adjust relative speed based on heading error.
                    if ((mStateTime.milliseconds() > 300 )) {
                        dblError = getDriveError(mdblRobotParm2);
                        dblSteer = getDriveSteer(dblError, mdblRobotParm3);

                        if (debug >= 3) {
                            fileLogger.writeEvent("runningDriveHeadingStep()", "dblError " + dblError);
                            fileLogger.writeEvent("runningDriveHeadingStep()", "dblSteer " + dblSteer);
                            fileLogger.writeEvent("runningDriveHeadingStep()", "Heading " + mdblRobotParm2);
                        }

                        // if driving in reverse, the motor correction also needs to be reversed
                        if (mdblStepDistance < 0)
                            dblSteer *= -1.0;

                        dblLeftSpeed = dblStepSpeedTemp - dblSteer;
                        dblRightSpeed = dblStepSpeedTemp + dblSteer;

                        // Normalize speeds if any one exceeds +/- 1.0;
                        dblMaxSpeed = Math.max(Math.abs(dblLeftSpeed), Math.abs(dblRightSpeed));
                        if (dblMaxSpeed > 1.0) {
                            dblLeftSpeed /= dblMaxSpeed;
                            dblRightSpeed /= dblMaxSpeed;
                        }
                    }
                } else {
                    dblLeftSpeed = dblStepSpeedTemp;
                    dblRightSpeed = dblStepSpeedTemp;
                }

                if ((mdblRobotParm1 == 2) || (mdblRobotParm4 == 2)) {
                    //use line sensor to stop robot when detected.
                    mdblInputLineSensor1 = LineSensor1.getVoltage();    //  Read the input pin
                    mdblInputLineSensor2 = LineSensor2.getVoltage();    //  Read the input pin
                    mdblInputLineSensor3 = LineSensor3.getVoltage();    //  Read the input pin

                    if ((mdblInputLineSensor1 < mdblWhiteThreshold) || (mdblInputLineSensor2 < mdblWhiteThreshold) || (mdblInputLineSensor3 < mdblWhiteThreshold)) {
                        //stop the motors we are complete
                        setDriveMotorPower(0);
                        mintCurrentDriveHeadingState = stepState.STATE_COMPLETE;
                    }

                }

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveHeadingStep()", "dblDistanceToEnd " + dblDistanceToEnd);
                }

                if (mblnRobotLastPos) {
                    if (dblDistanceToEnd <= 3.0) {
                        if (debug >= 2)
                        {
                            fileLogger.writeEvent("runningDriveHeadingStep()", "mblnRobotLastPos Complete         ");
                        }
                        mblnNextStepLastPos = true;
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentDriveHeadingState = stepState.STATE_COMPLETE;
                    }
                }

                //if within error margin stop
                if (robotDrive.leftMotor1.isBusy() && robotDrive.rightMotor1.isBusy())
                {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("runningDriveHeadingStep()", "Encoder counts per inch = " + COUNTS_PER_INCH + " dblDistanceFromStart " + dblDistanceFromStart + " dblDistanceToEnd " + dblDistanceToEnd + " Power Level " + dblStepSpeedTemp + " Running to target  L1, L2, R1, R2  " + mintStepLeftTarget1 + ", " + mintStepLeftTarget2 + ", " + mintStepRightTarget1 + ",  " + mintStepRightTarget2 + ", " + " Running at position L1 " + intLeft1MotorEncoderPosition + " L2 " + intLeft2MotorEncoderPosition + " R1 " + intRight1MotorEncoderPosition + " R2 " + intRight2MotorEncoderPosition);
                    }
                    telemetry.addData("Path1", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                    telemetry.addData("Path2", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intRight1MotorEncoderPosition);
                    telemetry.addData("Path3", "Running at %7d :%7d", intLeft2MotorEncoderPosition, intRight2MotorEncoderPosition);
                    // set power on motor controller to update speeds
                    setDriveLeftMotorPower(dblLeftSpeed);
                    setDriveRightMotorPower(dblRightSpeed);
                }
                else
                {
                    // Stop all motion;
                    setDriveMotorPower(0);
                    mblnBaseStepComplete = true;
                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("runningDriveHeadingStep()", "Complete         ");
                    }
                    mblnDisableVisionProcessing = false;  //enable vision processing
                    mintCurrentDriveHeadingState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }
    //--------------------------------------------------------------------------
    //  Execute the state.
    //--------------------------------------------------------------------------

    private void PivotTurnStep ()  //should be same as radius turn with radius of 1/2 robot width, so this function can be deleted once radius turn is completed
    {

        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;

        switch (mintCurrentPivotTurnState)
        {
            case STATE_INIT: {
                // set motor controller to mode
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mblnDisableVisionProcessing = true;  //disable vision processing

                mdblStepTurnL = 0;
                mdblStepTurnR = 0;

                switch (mdblRobotCommand.substring(0, 3)) {
                    case "LPE":
                        mdblStepTurnL = Double.parseDouble(mdblRobotCommand.substring(3));
                        mdblStepTurnR = 0;
                        break;
                    case "RPE":
                        mdblStepTurnL = 0;
                        mdblStepTurnR = Double.parseDouble(mdblRobotCommand.substring(3));
                        break;
                }
                if (debug >= 2)
                {
                    fileLogger.writeEvent("PivotTurnStep()", "mdblStepTurnL      :- " + mdblStepTurnL);
                    fileLogger.writeEvent("PivotTurnStep()", "mdblStepTurnR      :- " + mdblStepTurnR);
                }
                // Turn On RUN_TO_POSITION
                if(mdblStepTurnR == 0) {
                    // Determine new target position
                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("PivotTurnStep()", "Current LPosition:-" + robotDrive.leftMotor1.getCurrentPosition());
                    }

                    // Get Current Encoder positions
                    if (mblnNextStepLastPos) {
                        mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                        mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                        mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                        mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                    } else {
                        mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                        mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                        mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                        mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                    }
                    mblnNextStepLastPos = false;

                    // Determine new target position
                    mintStepLeftTarget1 = mintStartPositionLeft1 + (int) (mdblStepTurnL * COUNTS_PER_DEGREE);
                    mintStepLeftTarget2 = mintStartPositionLeft2 + (int) (mdblStepTurnL * COUNTS_PER_DEGREE);
                    mintStepRightTarget1 = mintStartPositionRight1 + (int) (mdblStepTurnR * COUNTS_PER_DEGREE);
                    mintStepRightTarget2 = mintStartPositionRight2 + (int) (mdblStepTurnR * COUNTS_PER_DEGREE);

                    //store the encoder positions so next step can calculate destination
                    mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                    mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                    mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                    mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("PivotTurnStep()", "mintStepLeftTarget1:-  " + mintStepLeftTarget1 + " mintStepLeftTarget2:-  " + mintStepLeftTarget2);
                    }
                    // pass target position to motor controller
                    robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                    robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);

                    // set motor controller to mode
                    robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                    // set power on motor controller to start moving
                    setDriveLeftMotorPower(Math.abs(mdblStepSpeed));
                }
                else {
                    // Determine new target position
                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("PivotTurnStep()", "Current RPosition:-" + robotDrive.rightMotor1.getCurrentPosition());
                    }

                    // Get Current Encoder positions
                    if (mblnNextStepLastPos) {
                        mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                        mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                        mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                        mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                    } else {
                        mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                        mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                        mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                        mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                    }
                    mblnNextStepLastPos = false;

                    // Determine new target position
                    mintStepLeftTarget1 = mintStartPositionLeft1 + (int) (mdblStepTurnL * COUNTS_PER_DEGREE);
                    mintStepLeftTarget2 = mintStartPositionLeft2 + (int) (mdblStepTurnL * COUNTS_PER_DEGREE);
                    mintStepRightTarget1 = mintStartPositionRight1 + (int) (mdblStepTurnR * COUNTS_PER_DEGREE);
                    mintStepRightTarget2 = mintStartPositionRight2 + (int) (mdblStepTurnR * COUNTS_PER_DEGREE);

                    //store the encoder positions so next step can calculate destination
                    mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                    mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                    mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                    mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                    if (debug >= 2)
                    {
                        fileLogger.writeEvent("PivotTurnStep()", "mintStepRightTarget1:- " + mintStepRightTarget1 + " mintStepRightTarget2:- " + mintStepRightTarget2);
                    }
                    // pass target position to motor controller
                    robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                    robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);

                    // set motor controller to mode, Turn On RUN_TO_POSITION
                    robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                    // set power on motor controller to start moving
                    setDriveRightMotorPower(Math.abs(mdblStepSpeed));
                }

                //store the encoder positions so next step can calculate destination
                mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                if (debug >= 3)
                {
                    fileLogger.writeEvent("PivotTurnStep()", "gblStepLeftTarget :- " + mintStepLeftTarget1 +  " mintStepLeftTarget2 :- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("PivotTurnStep()", "gblStepRightTarget:- " + mintStepRightTarget1 + " mintStepRightTarget2:- " + mintStepRightTarget2);
                }
                mintCurrentPivotTurnState = stepState.STATE_RUNNING;
            }
            break;
            case STATE_RUNNING: {

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                if (debug >= 3)
                {
                    fileLogger.writeEvent("PivotTurnStep()", "Running         ");
                    fileLogger.writeEvent("PivotTurnStep()", "Current LPosition1:-" + robotDrive.leftMotor1.getCurrentPosition() + " LTarget:- " + mintStepLeftTarget1 + " LPosition2:-" + robotDrive.leftMotor2.getCurrentPosition() + " LTarget2:- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("PivotTurnStep()", "Current RPosition1:-" + robotDrive.rightMotor1.getCurrentPosition() + " RTarget:- " + mintStepRightTarget1 + " RPosition2:-" + robotDrive.rightMotor2.getCurrentPosition() + " RTarget2:- " + mintStepRightTarget2);
                }

                if (mdblStepTurnR == 0) {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("PivotTurnStep()", "Running         ");
                    }
                    telemetry.addData("Target", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                    telemetry.addData("Actual_Left", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intLeft2MotorEncoderPosition);
                    telemetry.addData("ActualRight", "Running at %7d :%7d", intRight1MotorEncoderPosition, intRight2MotorEncoderPosition);
                    if (mblnRobotLastPos) {
                        if (((dblDistanceToEndLeft1 + dblDistanceToEndLeft2) / 2) < 2.0) {
                            mblnNextStepLastPos = true;
                            mblnDisableVisionProcessing = false;  //enable vision processing
                            mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
                        }
                    }
                    if (!robotDrive.leftMotor1.isBusy()) {
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent("PivotTurnStep()","Complete         " );
                        }
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
                    }
                } else if (mdblStepTurnL == 0) {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("PivotTurnStep()","Running         " );
                    }

                    telemetry.addData("Target", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                    telemetry.addData("Actual_Left", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intLeft2MotorEncoderPosition);
                    telemetry.addData("ActualRight", "Running at %7d :%7d", intRight1MotorEncoderPosition, intRight2MotorEncoderPosition);

                    if (mblnRobotLastPos) {
                        if (((dblDistanceToEndRight1 + dblDistanceToEndRight2) / 2) < 2.2) {
                            mblnNextStepLastPos = true;
                            mblnDisableVisionProcessing = false;  //enable vision processing
                            mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
                        }
                    }
                    if (!robotDrive.rightMotor1.isBusy()) {
                        if (debug >= 1)
                        {
                            fileLogger.writeEvent("PivotTurnStep()","Complete         " );
                        }
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
                    }
                } else {
                    // Stop all motion by setting power to 0
                    setDriveMotorPower(0);
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent("PivotTurnStep()","Complete         " );
                    }
                    mblnDisableVisionProcessing = false;  //enable vision processing
                    mintCurrentPivotTurnState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }

    private void TankTurnStep ()
    {
        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;

        switch (mintCurrentTankTurnState) {
            case STATE_INIT: {
                // set motor controller to mode
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mblnDisableVisionProcessing = true;  //disable vision processing

                mdblStepTurnL = 0;
                mdblStepTurnR = 0;

                // Get Current Encoder positions
                if (mblnNextStepLastPos) {
                    mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                    mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                    mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                    mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                } else {
                    mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                    mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                    mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                    mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                }
                mblnNextStepLastPos = false;

                // Determine new target position
                switch (mdblRobotCommand.substring(0, 3)) {
                    case "LTE":
                        mintStepLeftTarget1 = mintStartPositionLeft1 - (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepLeftTarget2 = mintStartPositionLeft2 - (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepRightTarget1 = mintStartPositionRight1 + (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepRightTarget2 = mintStartPositionRight2 + (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        break;
                    case "RTE":
                        mintStepLeftTarget1 = mintStartPositionLeft1 + (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepLeftTarget2 = mintStartPositionLeft2 + (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepRightTarget1 = mintStartPositionRight1 - (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        mintStepRightTarget2 = mintStartPositionRight2 - (int)(0.5 * Double.parseDouble(mdblRobotCommand.substring(3)) * COUNTS_PER_DEGREE);
                        break;
                }

                //store the encoder positions so next step can calculate destination
                mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                if (debug >= 3)
                {
                    fileLogger.writeEvent("TankTurnStep()","Current LPosition1:- " + robotDrive.leftMotor1.getCurrentPosition() + " mintStepLeftTarget1:-   " + mintStepLeftTarget1 );
                    fileLogger.writeEvent("TankTurnStep()","Current LPosition2:- " + robotDrive.leftMotor2.getCurrentPosition() + " mintStepLeftTarget2:-   " + mintStepLeftTarget2 );
                    fileLogger.writeEvent("TankTurnStep()","Current RPosition1:- " + robotDrive.rightMotor1.getCurrentPosition() + " mintStepRightTarget1:- " + mintStepRightTarget1 );
                    fileLogger.writeEvent("TankTurnStep()","Current RPosition2:- " + robotDrive.rightMotor2.getCurrentPosition() + " mintStepRightTarget2:- " + mintStepRightTarget2 );
                }

                // pass target position to motor controller
                robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);
                robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);
                // set motor controller to mode
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                // set power on motor controller to start moving
                setDriveMotorPower(Math.abs(mdblStepSpeed));
                if (debug >= 2)
                {
                    fileLogger.writeEvent("TankTurnStep()","mintStepLeftTarget1 :- " + mintStepLeftTarget1  );
                    fileLogger.writeEvent("TankTurnStep()","mintStepLeftTarget2 :- " + mintStepLeftTarget2  );
                    fileLogger.writeEvent("TankTurnStep()","mintStepRightTarget1:- " + mintStepRightTarget1  );
                    fileLogger.writeEvent("TankTurnStep()","mintStepRightTarget2:- " + mintStepRightTarget2  );
                }

                mintCurrentTankTurnState = stepState.STATE_RUNNING;
            }
            break;
            case STATE_RUNNING: {

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                if (debug >= 3)
                {
                    fileLogger.writeEvent("TankTurnStep()","Running         " );
                    fileLogger.writeEvent("TankTurnStep()","Current LPosition1:- " + intLeft1MotorEncoderPosition + " LTarget1:- " + mintStepLeftTarget1);
                    fileLogger.writeEvent("TankTurnStep()","Current LPosition2:- " + intLeft2MotorEncoderPosition + " LTarget2:- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("TankTurnStep()","Current RPosition1:- " + intRight1MotorEncoderPosition + " RTarget1:- " + mintStepRightTarget1);
                    fileLogger.writeEvent("TankTurnStep()","Current RPosition2:- " + intRight2MotorEncoderPosition + " RTarget2:- " + mintStepRightTarget2);
                }

                telemetry.addData("Target", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                telemetry.addData("Actual_Left", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intLeft2MotorEncoderPosition);
                telemetry.addData("ActualRight", "Running at %7d :%7d", intRight1MotorEncoderPosition, intRight2MotorEncoderPosition);

                if (mblnRobotLastPos) {
                    if (((Math.abs(dblDistanceToEndRight1 + dblDistanceToEndRight2) / 2) < 2.2) && ((Math.abs(dblDistanceToEndLeft1 + dblDistanceToEndLeft2) / 2) < 2.2)) {
                        mblnNextStepLastPos = true;
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentTankTurnState = stepState.STATE_COMPLETE;
                    }
                }

                if (!robotDrive.leftMotor1.isBusy() || (!robotDrive.rightMotor1.isBusy()))
                {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("TankTurnStep()","Complete         " );
                    }
                    setDriveMotorPower(0);
                    mblnDisableVisionProcessing = false;  //enable vision processing
                    mintCurrentTankTurnState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }

    //this has not been programmed, do not use
    private void RadiusTurnStep ()
    {
        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;

        switch (mintCurrentRadiusTurnState) {
            case STATE_INIT: {

                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mblnDisableVisionProcessing = true;  //disable vision processing

                mdblRobotTurnAngle = Double.parseDouble(mdblRobotCommand.substring(3));
                Log.d(TAG, "RadiusTurnStep mdblRobotTurnAngle" + mdblRobotTurnAngle );
                if (debug >= 3)
                {
                    fileLogger.writeEvent("RadiusTurnStep()","mdblRobotTurnAngle" + mdblRobotTurnAngle );
                }

                //calculate the distance to travel based on the angle we are turning
                // length = radius x angle (in radians)
                mdblArcLengthRadiusTurnOuter = ((Double.parseDouble(mdblRobotCommand.substring(3)) / 180) *  Math.PI) * mdblRobotParm1;
                mdblArcLengthRadiusTurnInner = ((Double.parseDouble(mdblRobotCommand.substring(3)) / 180) *  Math.PI) * (mdblRobotParm1 - (ROBOT_TRACK));
                //mdblArcLengthRadiusTurnOuter = ((Double.parseDouble(mdblRobotCommand.substring(3)) / 180) *  Math.PI) * (mdblRobotParm1 + (0.5 * ROBOT_TRACK));

                mdblSpeedOuter =  mdblStepSpeed;

                if (mdblSpeedOuter >= 0.58) {
                    mdblSpeedOuter = 0.58;  //This is the maximum speed, anything above 0.6 is the same as a speed of 1 for drive to position
                }
                mdblSpeedInner = mdblArcLengthRadiusTurnInner / mdblArcLengthRadiusTurnOuter * mdblSpeedOuter * 0.96;

                if (debug >= 3)
                {
                    Log.d(TAG, "RadiusTurnStep mdblArcLengthRadiusTurnInner " + mdblArcLengthRadiusTurnInner );
                    Log.d(TAG, "RadiusTurnStep mdblArcLengthRadiusTurnOuter " + mdblArcLengthRadiusTurnOuter );
                    Log.d(TAG, "RadiusTurnStep mdblSpeedOuter " + mdblSpeedOuter );
                    Log.d(TAG, "RadiusTurnStep mdblSpeedInner " + mdblSpeedInner );
                    fileLogger.writeEvent("RadiusTurnStep()","mdblArcLengthRadiusTurnInner " + mdblArcLengthRadiusTurnInner );
                    fileLogger.writeEvent("RadiusTurnStep()","mdblArcLengthRadiusTurnOuter " + mdblArcLengthRadiusTurnOuter );
                    fileLogger.writeEvent("RadiusTurnStep()","mdblSpeedOuter " + mdblSpeedOuter );
                    fileLogger.writeEvent("RadiusTurnStep()","mdblSpeedInner " + mdblSpeedInner );
                }

                // Get Current Encoder positions
                if (mblnNextStepLastPos) {
                    mintStartPositionLeft1 = mintLastEncoderDestinationLeft1;
                    mintStartPositionLeft2 = mintLastEncoderDestinationLeft2;
                    mintStartPositionRight1 = mintLastEncoderDestinationRight1;
                    mintStartPositionRight2 = mintLastEncoderDestinationRight2;
                } else {
                    mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                    mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                    mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                    mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();
                }
                mblnNextStepLastPos = false;

                // Determine new target position
                switch (mdblRobotCommand.substring(0, 3)) {
                    case "LRE":
                        mintStepLeftTarget1 = mintStartPositionLeft1 + (int)(mdblArcLengthRadiusTurnInner * COUNTS_PER_INCH);
                        mintStepLeftTarget2 = mintStartPositionLeft2 + (int)(mdblArcLengthRadiusTurnInner * COUNTS_PER_INCH);
                        mintStepRightTarget1 = mintStartPositionRight1 + (int)(mdblArcLengthRadiusTurnOuter * COUNTS_PER_INCH);
                        mintStepRightTarget2 = mintStartPositionRight2 + (int)(mdblArcLengthRadiusTurnOuter * COUNTS_PER_INCH);

                        //store the encoder positions so next step can calculate destination
                        mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                        mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                        mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                        mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                        // pass target position to motor controller
                        robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                        robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);
                        robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                        robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);

                        // set motor controller to mode
                        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                        // set power on motor controller to start moving
                        setDriveLeftMotorPower(mdblSpeedInner);  //left side is inner when turning left
                        setDriveRightMotorPower(mdblSpeedOuter);  //right side is outer when turning left
                        break;
                    case "RRE":
                        mintStepLeftTarget1 = mintStartPositionLeft1 + (int)(mdblArcLengthRadiusTurnOuter * COUNTS_PER_INCH);
                        mintStepLeftTarget2 = mintStartPositionLeft2 + (int)(mdblArcLengthRadiusTurnOuter * COUNTS_PER_INCH);
                        mintStepRightTarget1 = mintStartPositionRight1 + (int)(mdblArcLengthRadiusTurnInner * COUNTS_PER_INCH);
                        mintStepRightTarget2 = mintStartPositionRight2 + (int)(mdblArcLengthRadiusTurnInner * COUNTS_PER_INCH);

                        //store the encoder positions so next step can calculate destination
                        mintLastEncoderDestinationLeft1 = mintStepLeftTarget1;
                        mintLastEncoderDestinationLeft2 = mintStepLeftTarget2;
                        mintLastEncoderDestinationRight1 = mintStepRightTarget1;
                        mintLastEncoderDestinationRight2 = mintStepRightTarget2;

                        // pass target position to motor controller
                        robotDrive.leftMotor1.setTargetPosition(mintStepLeftTarget1);
                        robotDrive.leftMotor2.setTargetPosition(mintStepLeftTarget2);
                        robotDrive.rightMotor1.setTargetPosition(mintStepRightTarget1);
                        robotDrive.rightMotor2.setTargetPosition(mintStepRightTarget2);

                        // set motor controller to mode
                        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                        // set power on motor controller to start moving
                        setDriveLeftMotorPower(mdblSpeedOuter);  //left side is outer when turning left
                        setDriveRightMotorPower(mdblSpeedInner);  //right side is inner when turning left
                        break;
                }

                if (debug >= 3)
                {
                    fileLogger.writeEvent("RadiusTurnStep()","Current LPosition1:- " + robotDrive.leftMotor1.getCurrentPosition() + " mintStepLeftTarget1:-   " + mintStepLeftTarget1 );
                    fileLogger.writeEvent("RadiusTurnStep()","Current LPosition2:- " + robotDrive.leftMotor2.getCurrentPosition() + " mintStepLeftTarget2:-   " + mintStepLeftTarget2 );
                    fileLogger.writeEvent("RadiusTurnStep()","Current RPosition1:- " + robotDrive.rightMotor1.getCurrentPosition() + " mintStepRightTarget1:- " + mintStepRightTarget1 );
                    fileLogger.writeEvent("RadiusTurnStep()","Current RPosition2:- " + robotDrive.rightMotor2.getCurrentPosition() + " mintStepRightTarget2:- " + mintStepRightTarget2 );
                }
                mintCurrentRadiusTurnState = stepState.STATE_RUNNING;
            }
            break;
            case STATE_RUNNING:
            {

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                if (debug >= 3)
                {
                    fileLogger.writeEvent("RadiusTurnStep()","Running         " );
                    fileLogger.writeEvent("RadiusTurnStep()","Current LPosition1:- " + intLeft1MotorEncoderPosition + " LTarget1:- " + mintStepLeftTarget1);
                    fileLogger.writeEvent("RadiusTurnStep()","Current LPosition2:- " + intLeft2MotorEncoderPosition + " LTarget2:- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("RadiusTurnStep()","Current RPosition1:- " + intRight1MotorEncoderPosition + " RTarget1:- " + mintStepRightTarget1);
                    fileLogger.writeEvent("RadiusTurnStep()","Current RPosition2:- " + intRight2MotorEncoderPosition + " RTarget2:- " + mintStepRightTarget2);
                }

                telemetry.addData("Target", "Running to %7d :%7d", mintStepLeftTarget1, mintStepRightTarget1);
                telemetry.addData("Actual_Left", "Running at %7d :%7d", intLeft1MotorEncoderPosition, intLeft2MotorEncoderPosition);
                telemetry.addData("ActualRight", "Running at %7d :%7d", intRight1MotorEncoderPosition, intRight2MotorEncoderPosition);

                if (mblnRobotLastPos) {
                    if ((((dblDistanceToEndRight1 + dblDistanceToEndRight2) / 2) < 2) && (((dblDistanceToEndLeft1 + dblDistanceToEndLeft2) / 2) < 2)) {
                        mblnNextStepLastPos = true;
                        mblnDisableVisionProcessing = false;  //enable vision processing
                        mintCurrentRadiusTurnState = stepState.STATE_COMPLETE;
                    }
                }

                if (!robotDrive.leftMotor1.isBusy() || (!robotDrive.rightMotor1.isBusy()))
                {
                    setDriveMotorPower(0);
                    if (debug >= 1)
                    {
                        fileLogger.writeEvent("RadiusTurnStep()","Complete         " );
                    }
                    mblnDisableVisionProcessing = false;  //enable vision processing
                    mintCurrentRadiusTurnState = stepState.STATE_COMPLETE;
                }

            }
            break;
        }
    }

    private void gyroTurnBasic()
    {
        switch (mintCurrentTankTurnGyroBasicState){
            case STATE_INIT:
            {
                if (debug >= 3)
                {
                    fileLogger.writeEvent("setTurnRobot()","INIT");
                }
                mdblRobotTurnAngle = Double.parseDouble(mdblRobotCommand.substring(3));
                mintCurrentTankTurnGyroBasicState = stepState.STATE_RUNNING;
                mdblTurnAbsoluteGyro = mdblRobotTurnAngle + (gyro.getIntegratedZValue() * GYRO_CORRECTION_MULTIPLIER); //Target
            }//end case INIT
            break;
            case STATE_RUNNING:
            {
                mdblGyrozAccumulated = gyro.getIntegratedZValue() * GYRO_CORRECTION_MULTIPLIER;  //Set variables to gyro readings
                if (debug >= 3)
                {
                    fileLogger.writeEvent("setTurnRobot()","gyrozAccumulated="+ mdblGyrozAccumulated);
                    fileLogger.writeEvent("setTurnRobot()","RUNNING");
                    fileLogger.writeEvent("setTurnRobot()","Robot Turn Angle="+ mdblRobotTurnAngle);
                    fileLogger.writeEvent("setTurnRobot()","Robot Turn Target="+ mdblTurnAbsoluteGyro);
                    fileLogger.writeEvent("setTurnRobot()","STATE"  + mintCurrentTankTurnGyroBasicState);
                    fileLogger.writeEvent("setTurnRobot()","Turn Speed"  + mdblStepSpeed);
                }
                if (Math.abs(mdblGyrozAccumulated - mdblTurnAbsoluteGyro) > 4) {  //Continue while the robot direction is further than three degrees from the target
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("setTurnRobot()","Turning Robot="+ mdblGyrozAccumulated);
                        fileLogger.writeEvent("setTurnRobot()","Turning Angel="+ mdblTurnAbsoluteGyro);
                    }
                    if (mdblGyrozAccumulated > mdblTurnAbsoluteGyro) {  //if gyro is positive, we will turn right
                        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        setDriveLeftMotorPower(mdblStepSpeed);
                        setDriveRightMotorPower(-mdblStepSpeed);
                        if (debug >= 3) {
                            fileLogger.writeEvent("setTurnRobot()","TURN RIGHT gyrozAccumulated > turnAbsolute");
                        }
                    }

                    if (mdblGyrozAccumulated < mdblTurnAbsoluteGyro) {  //if gyro is positive, we will turn left
                        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                        setDriveLeftMotorPower(-mdblStepSpeed);
                        setDriveRightMotorPower(mdblStepSpeed);
                        if (debug >= 3) {
                            fileLogger.writeEvent("setTurnRobot()","TURN LEFT gyrozAccumulated < turnAbsolute");
                        }
                    }
                    //gyrozAccumulated = gyro.getIntegratedZValue() * GYRO_CORRECTION_MULTIPLIER;;  //Set variables to gyro readings
                    //telemetry.addData("1. accu", gyrozAccumulated);
                }else {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("setTurnRobot()","TURN COMPLETE");
                    }
                    setDriveMotorPower(0);
                    mintCurrentTankTurnGyroBasicState = stepState.STATE_COMPLETE;
                }
            }//end case RUNNING
            break;
        }//end SWITCH
    }//end METHOD


    private void TankTurnGyroHeading()
    {
        switch (mintCurrentTankTurnGyroHeadingState){
            case STATE_INIT:
            {
                mdblPowerBoost = 0;
                mintStableCount = 0;
                mstrWiggleDir = "";
                mdblRobotTurnAngle = Double.parseDouble(mdblRobotCommand.substring(3));
                Log.d(TAG, "mdblRobotTurnAngle " + mdblRobotTurnAngle + " gyro.getHeading() " + gyro.getHeading());
                mdblTurnAbsoluteGyro = Double.parseDouble(newAngleDirection (gyro.getHeading(), (int)mdblRobotTurnAngle).substring(3));
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

                mintCurrentTankTurnGyroHeadingState = stepState.STATE_RUNNING;
            }
            break;
            case STATE_RUNNING: {
                //mdblGyrozAccumulated = Math.abs(gyro.getIntegratedZValue() * GYRO_CORRECTION_MULTIPLIER);  //Set variables to gyro readings

                mdblGyrozAccumulated = gyro.getHeading();
                mdblGyrozAccumulated = teamAngleAdjust(mdblGyrozAccumulated);//Set variables to gyro readings
                mdblTurnAbsoluteGyro = Double.parseDouble(newAngleDirectionGyro ((int)mdblGyrozAccumulated, (int)mdblRobotTurnAngle).substring(3));
                String mstrDirection = (newAngleDirectionGyro ((int)mdblGyrozAccumulated, (int)mdblRobotTurnAngle).substring(0, 3));
                if (debug >= 3) {
                    fileLogger.writeEvent("TankTurnGyro()", "Running, mdblGyrozAccumulated = " + mdblGyrozAccumulated);
                    fileLogger.writeEvent("TankTurnGyro()", "Running, mdblTurnAbsoluteGyro = " + mdblTurnAbsoluteGyro);
                    fileLogger.writeEvent("TankTurnGyro()", "Running, mstrDirection        = " + mstrDirection);
                }

                if (Math.abs(mdblTurnAbsoluteGyro) > 21) {  //Continue while the robot direction is further than three degrees from the target
                    mintStableCount = 0;
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("TankTurnGyro()","High Speed.....");
                    }
                    if (mstrDirection.equals("LTE"))  //want to turn left
                    {
                        if (debug >= 3)
                        {
                            fileLogger.writeEvent("TankTurnGyro()","Left Turn.....");
                        }
                        if (mstrWiggleDir.equals("RTE")) {
                            mdblPowerBoost = mdblPowerBoost - 0.01;
                            mintPowerBoostCount = 0;
                        }
                        mstrWiggleDir = "LTE";
                        setDriveLeftMotorPower(mdblStepSpeed);
                        setDriveRightMotorPower(-mdblStepSpeed);
                    }
                    else if (mstrDirection.equals("RTE"))  //want to turn left
                    {
                        if (mstrWiggleDir.equals("LTE")) {
                            mdblPowerBoost = mdblPowerBoost - 0.01;
                            mintPowerBoostCount = 0;
                        }
                        mstrWiggleDir = "RTE";
                        if (debug >= 3)
                        {
                            fileLogger.writeEvent("TankTurnGyro()","Right Turn.....");
                        }
                        setDriveLeftMotorPower(-mdblStepSpeed);
                        setDriveRightMotorPower(mdblStepSpeed);
                    }
                }
                else if (Math.abs(mdblTurnAbsoluteGyro) > mdblRobotParm1) {  //Continue while the robot direction is further than three degrees from the target
                    mintStableCount = 0;
                    mintPowerBoostCount++;
                    if (mintPowerBoostCount > 50)
                    {
                        mdblPowerBoost = mdblPowerBoost + 0.01;
                        mintPowerBoostCount = 0;
                    }
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("TankTurnGyro()","Slow Speed Nearing final angle.....");
                    }
                    if (mstrDirection.equals("LTE") )  //want to turn left
                    {
                        if (mstrWiggleDir.equals("RTE")) {
                            mdblPowerBoost = mdblPowerBoost - 0.01;
                            mintPowerBoostCount = 0;
                        }
                        mstrWiggleDir = "LTE";

                        if (debug >= 3)
                        {
                            fileLogger.writeEvent("TankTurnGyro()","Left Turn.....");
                        }
                        setDriveLeftMotorPower(.12 + mdblPowerBoost);
                        setDriveRightMotorPower(-(0.12 + mdblPowerBoost));
                    }
                    else if (mstrDirection.equals("RTE") )  //want to turn left
                    {
                        if (mstrWiggleDir.equals("LTE")) {
                            mdblPowerBoost = mdblPowerBoost - 0.01;
                            mintPowerBoostCount = 0;
                        }
                        mstrWiggleDir = "RTE";
                        if (debug >= 3)
                        {
                            fileLogger.writeEvent("TankTurnGyro()","Right Turn.....");
                        }
                        setDriveLeftMotorPower(-(0.12 + mdblPowerBoost));
                        setDriveRightMotorPower(0.12 + mdblPowerBoost);
                    }
                }
                else
                {
                    mintStableCount++;
                    if (mintStableCount > 20) {
                        if (debug >= 3) {
                            fileLogger.writeEvent("TankTurnGyro()", "Complete.....");
                        }
                        robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        setDriveMotorPower(0);
                        mintCurrentTankTurnGyroHeadingState = stepState.STATE_COMPLETE;
                    }
                }
            }
            break;
        }
    }

    private void TankTurnGyroHeadingEncoder()
    {
        switch (mint5291GyroTurnEncoderState){
            case STATE_INIT:
            {
                mdblPowerBoost = 0;
                mintStableCount = 0;
                mstrWiggleDir = "";

                if ((mStateTime.milliseconds() > 300 )) {
                    mdblRobotTurnAngle = Double.parseDouble(mdblRobotCommand.substring(3));
                    Log.d(TAG, "mdblRobotTurnAngle " + mdblRobotTurnAngle + " gyro.getHeading() " + gyro.getHeading());
                    mdblTurnAbsoluteGyro = Double.parseDouble(newAngleDirection(gyro.getHeading(), (int) mdblRobotTurnAngle).substring(3));

                    mint5291GyroTurnEncoderState = stepState.STATE_RUNNING;
                }
            }
            break;
            case STATE_RUNNING: {
                //mdblGyrozAccumulated = Math.abs(gyro.getIntegratedZValue() * GYRO_CORRECTION_MULTIPLIER);  //Set variables to gyro readings

                mdblGyrozAccumulated = gyro.getHeading();
                mdblGyrozAccumulated = teamAngleAdjust(mdblGyrozAccumulated);//Set variables to gyro readings
                mdblTurnAbsoluteGyro = Double.parseDouble(newAngleDirectionGyro ((int)mdblGyrozAccumulated, (int)mdblRobotTurnAngle).substring(3));
                String mstrDirection = (newAngleDirectionGyro ((int)mdblGyrozAccumulated, (int)mdblRobotTurnAngle).substring(0, 3));
                if (debug >= 3) {
                    fileLogger.writeEvent("TankTurnGyroHeadingEncoder()", "Running, mdblGyrozAccumulated = " + mdblGyrozAccumulated);
                    fileLogger.writeEvent("TankTurnGyroHeadingEncoder()", "Running, mdblTurnAbsoluteGyro = " + mdblTurnAbsoluteGyro);
                    fileLogger.writeEvent("TankTurnGyroHeadingEncoder()", "Running, mstrDirection        = " + mstrDirection);
                }
                insertSteps(10, newAngleDirectionGyro ((int)mdblGyrozAccumulated, (int)mdblRobotTurnAngle), false, false, 0, 0, 0, 0, 0, 0, mdblStepSpeed, mintCurrentStep + 1);
                mint5291GyroTurnEncoderState = stepState.STATE_COMPLETE;
            }
            break;
        }
    }

    private void VuforiaLocalise ()
    {
        switch (mintCurrentVuforiaState) {
            case STATE_INIT: {
                //ensure vision processing is enabled
                mblnDisableVisionProcessing = false;  //enable vision processing

                mintCurrentVuforiaState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("mintCurrentVuforiaState()", "Initialised");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (debug >= 2) {
                    fileLogger.writeEvent("mintCurrentVuforiaState()", "Running" );
                }
                String strCorrectionAngle = "";
                if (debug >= 2) {
                    fileLogger.writeEvent("mintCurrentVuforiaState()", "localiseRobotPos " + localiseRobotPos );
                }
                if (!localiseRobotPos) {

                    //need to rerun this step as we cannot get localisation and need to adjust robot to see if we can see a target
                    insertSteps(10, "VFL", false, false, 0,    0,    0,    0,    0,    0,  0.5, mintCurrentStep + 1);
                    if (debug >= 2) {
                        fileLogger.writeEvent("mintCurrentVuforiaState()", "Not Localised, inserting a new step" );
                    }
                    //need a delay, as Vuforia is slow to update
                    insertSteps(2, "DEL500", false, false, 0, 0, 0, 0, 0, 0, 0, mintCurrentStep + 1);

                    //need to adjust robot so we can see target, lets turn robot 180 degrees, if we are facing RED drivers we will end up facing BLUE targets,
                    //if we are facing blue drives we will end up facing RED targets.
                    //if we can't localise we need to abort autonomous so lets try a few things to see if we can localise,
                    // first we will try turning around,
                    // second we will move forward 2 feet
                    // third - abort
                    //Parameter 1 - stop turning once localisation is achieved
                    insertSteps(10, "RTE135", false, true, 1,    0,    0,    0,    0,    0,  0.5, mintCurrentStep + 1);
                    mintCurrentVuforiaState = stepState.STATE_COMPLETE;
                    break;
                }

                int intLocalisedRobotBearing = (int)localisedRobotBearing;

                if (debug >= 2) {
                    fileLogger.writeEvent("mintCurrentVuforiaState()", "Localised, determining angles.... intLocalisedRobotBearing= " + intLocalisedRobotBearing + " Alliancecolour= " + allianceColor);
                }
                //vuforia angles or 0 towards the BLUE drivers, AStar 0 is to the BLUE beacons
                if (allianceColor.equals("Red")) {
                    //double check localisation
                    if ((intLocalisedRobotBearing > 3) && (intLocalisedRobotBearing < 177))
                    {
                        strCorrectionAngle = "LTE" + (180 - intLocalisedRobotBearing);
                    }
                    else if ((intLocalisedRobotBearing > 183) && (intLocalisedRobotBearing < 357))
                    {
                        strCorrectionAngle = "RTE" + (180 - (360 - intLocalisedRobotBearing));
                    }
                    else
                    {
                        mintCurrentVuforiaState = stepState.STATE_COMPLETE;
                        break;
                    }
                    //double check localisation
                    if (debug >= 2) {
                        fileLogger.writeEvent("mintCurrentVuforiaState()", "Inserting Steps VFL 0 0 0 mintCurrentStep " + mintCurrentStep );
                    }
                    insertSteps(10, "VFL", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                    //need a delay, as Vuforia is slow to update
                    insertSteps(2, "DEL500", false, false, 0, 0, 0, 0, 0, 0, 0, mintCurrentStep + 1);
                    //load the angle to adjust
                    insertSteps(10, strCorrectionAngle, false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                    mintCurrentVuforiaState = stepState.STATE_COMPLETE;
                    break;
                }

                if (allianceColor.equals("Blue")) {
                    if ((intLocalisedRobotBearing > 273) && (intLocalisedRobotBearing < 360))
                    {
                        strCorrectionAngle = "LTE" + (intLocalisedRobotBearing - 270);
                    }
                    else if ((intLocalisedRobotBearing > 0) && (intLocalisedRobotBearing < 91))
                    {
                        strCorrectionAngle = "LTE" + (90 + intLocalisedRobotBearing);
                    }
                    else if ((intLocalisedRobotBearing > 90) && (intLocalisedRobotBearing < 267))
                    {
                        strCorrectionAngle = "RTE" + (270 - intLocalisedRobotBearing);
                    }
                    else
                    {
                        mintCurrentVuforiaState = stepState.STATE_COMPLETE;
                        break;
                    }
                    insertSteps(10, "VFL", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                    //need a delay, as Vuforia is slow to update
                    insertSteps(2, "DEL500", false, false, 0, 0, 0, 0, 0, 0, 0, mintCurrentStep + 1);
                    //load the angle to adjust
                    insertSteps(10, strCorrectionAngle, false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                    mintCurrentVuforiaState = stepState.STATE_COMPLETE;
                    break;
                }
            }
            break;
        }
    }

    private void VuforiaMove ()
    {
        switch (mintCurrentVuforiaMoveState) {
            case STATE_INIT: {
                //ensure vision processing is enable
                mblnDisableVisionProcessing = false;  //enable vision processing

                mintCurrentVuforiaMoveState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaMove()", "Initialised");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaMove()", "Running" );
                }
                String strCorrectionAngle = "";
                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaMove()", "localiseRobotPos " + localiseRobotPos );
                }
                if (!localiseRobotPos)
                {
                    //need to do something gere to try and get localise
                    mintCurrentVuforiaMoveState = stepState.STATE_COMPLETE;
                    break;
                }

                int currentX = (int)localisedRobotX;
                int currentY = (int)localisedRobotY;
                int intLocalisedRobotBearing = (int)localisedRobotBearing;
                double requiredMoveX = (currentX - (int)mdblRobotParm4);
                double requiredMoveY = (currentY - (int)mdblRobotParm5);

                double requiredMoveDistance = ((Math.sqrt(requiredMoveX * requiredMoveX + requiredMoveY * requiredMoveY)) / 25.4);

                double requiredMoveAngletemp1 = requiredMoveX/requiredMoveY;
                double requiredMoveAngletemp2 = Math.atan(requiredMoveAngletemp1);
                double requiredMoveAngletemp3 = Math.toDegrees(requiredMoveAngletemp2);
                int requiredMoveAngle = (int)Math.abs(requiredMoveAngletemp3);

                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaMove()", "Temp Values requiredMoveAngletemp1 " + requiredMoveAngletemp1 + " requiredMoveAngletemp2 " + requiredMoveAngletemp2 + " requiredMoveAngletemp3 " + requiredMoveAngletemp3);
                    fileLogger.writeEvent("VuforiaMove()", "Temp Values currentX " + currentX + " currentY " + currentY);
                    fileLogger.writeEvent("VuforiaMove()", "Localised, determining angles....Alliancecolour= " + allianceColor + " intLocalisedRobotBearing= " + intLocalisedRobotBearing + " CurrentX= " + currentX + " CurrentY= " + currentY);
                    fileLogger.writeEvent("VuforiaMove()", "Localised, determining angles....requiredMoveX " + requiredMoveX + " requiredMoveY " + requiredMoveY);
                    fileLogger.writeEvent("VuforiaMove()", "Localised, determining angles....requiredMoveDistance " + requiredMoveDistance + " requiredMoveAngle " + requiredMoveAngle);
                }

                if ((((int) mdblRobotParm5) > currentY) && ((int) mdblRobotParm4 > currentX)) {
                    requiredMoveAngle = 90 - requiredMoveAngle;
                } else if ((((int) mdblRobotParm5) > currentY) && ((int) mdblRobotParm4 < currentX)) {
                    requiredMoveAngle =  90 + requiredMoveAngle;
                } else if ((((int) mdblRobotParm5) < currentY) && ((int) mdblRobotParm4 > currentX)) {
                    requiredMoveAngle = 270 + requiredMoveAngle;
                } else if ((((int) mdblRobotParm5) < currentY) && ((int) mdblRobotParm4 < currentX)) {
                    requiredMoveAngle = 270 - requiredMoveAngle;
                }

                strCorrectionAngle = newAngleDirection (intLocalisedRobotBearing, requiredMoveAngle);

                insertSteps(10, "FWE"+requiredMoveDistance, false, false, 0, 0, 0, 0, 0, 0, 0.6, mintCurrentStep + 1);
                insertSteps(10, strCorrectionAngle, false, false, 0, 0, 0, 0, 0, 0, 0.4, mintCurrentStep + 1);

                mintCurrentVuforiaMoveState = stepState.STATE_COMPLETE;
            }
            break;
        }
    }

    private void VuforiaTurn ()
    {
        String strCorrectionAngle = "";

        switch (mintCurrentVuforiaTurnState) {
            case STATE_INIT: {
                //ensure vision processing is enabled
                mblnDisableVisionProcessing = false;  //enable vision processing

                mintCurrentVuforiaTurnState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaTurn()", "Initialised");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaTurn()", "Running" );
                }

                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaTurn()", "localiseRobotPos " + localiseRobotPos );
                }
                if (!localiseRobotPos)
                {
                    //need to do something here to try and get localised
                    mintCurrentVuforiaTurnState = stepState.STATE_COMPLETE;
                    break;
                }

                int intLocalisedRobotBearing = (int)localisedRobotBearing;
                double requiredMoveAngle = mdblRobotParm1;
                strCorrectionAngle = newAngleDirection (intLocalisedRobotBearing, (int)requiredMoveAngle);

                if (debug >= 2) {
                    fileLogger.writeEvent("VuforiaTurn()", "Localised, determining angles....Alliancecolour= " + allianceColor + " intLocalisedRobotBearing= " + intLocalisedRobotBearing  + " requiredMoveAngle " + requiredMoveAngle);
                }

                insertSteps(10, strCorrectionAngle, false, false, 0, 0, 0, 0, 0, 0, 0.4, mintCurrentStep + 1);

                mintCurrentVuforiaTurnState = stepState.STATE_COMPLETE;
            }
            break;
        }
    }

    private void BeaconColour() {

        switch (mintCurrentBeaconColourState) {
            case STATE_INIT: {
                //ensure vision processing is enable
                mblnDisableVisionProcessing = false;  //enable vision processing
                readyToCapture = true;               //let OpenCV start doing its thing
                mintNumberColourTries = 0;
                mintCurrentBeaconColourState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("BeaconColour()", "Initialised");
                    Log.d("BeaconColour()", "Initialised");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (debug >= 2) {
                    fileLogger.writeEvent("BeaconColour()", "Running" );
                    Log.d("BeaconColour()", "Running" );
                }
                mintNumberColourTries ++;

                if (debug >= 2) {
                    fileLogger.writeEvent("BeaconColour()", "Returned " + mColour + " mintNumberColourTries" + mintNumberColourTries);
                    Log.d("BeaconColour()", "Returned " + mColour + " mintNumberColourTries" + mintNumberColourTries);
                }

                if (mintNumberColourTries > 2) {
                    readyToCapture = false;
                    mintCurrentBeaconColourState = stepState.STATE_COMPLETE;
                }

                if (allianceColor.equals("Red")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME + 90, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 7, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);


                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 90, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);


                    } else if (mColour == Constants.BeaconColours.BEACON_RED)
                    {


                    } else if (mColour == Constants.BeaconColours.BEACON_BLUE)
                    {


                    }
                } else if (allianceColor.equals("Blue")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME , SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 90, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);


                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME + 90, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);


                    }  else if (mColour == Constants.BeaconColours.BEACON_RED)
                    {


                    } else if (mColour == Constants.BeaconColours.BEACON_BLUE)
                    {


                    }
                }
                mint5291LEDStatus = LEDState.STATE_BEACON;
            }
            break;
        }
    }

    private void AttackBeacon() {

        double dblMaxDistance = 0;
        boolean blnColourOK = false;

        switch (mintCurrentAttackBeaconState) {
            case STATE_INIT: {
                //ensure vision processing is enable
                mblnDisableVisionProcessing = false;  //enable vision processing
                readyToCapture = false;               //stop OpenCV start doing its thing
                mintNumberColourTries = 0;
                mintCurrentAttackBeaconState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("AttackBeacon()", "Initialised");
                    Log.d("AttackBeacon()", "Initialised");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (debug >= 2) {
                    fileLogger.writeEvent("AttackBeacon()", "Running" );
                    Log.d("AttackBeacon()", "Running" );
                }

                /*//the button is a few degrees to the left or right.
                if (allianceColor.equals("Red")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        insertSteps(10, "FWE-24", false, false, 0, 0, 0, 0, 0, 0, 0.6, mintCurrentStep + 1);
                        insertSteps(10, "FWE15", false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                        //insertSteps(10, "LTE20", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        //insertSteps(10, "FWE1", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        //insertSteps(10, "RTE20", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        insertSteps(10, "FWE-24", false, false, 0, 0, 0, 0, 0, 0, 0.6, mintCurrentStep + 1);
                        insertSteps(10, "FWE10", false, false, 0, 0, 0, 0, 0, 0, 0.35, mintCurrentStep + 1);
                        //insertSteps(10, "RTE45", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        insertSteps(10, "GTH0", false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                        insertSteps(10, "FWE5", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        //insertSteps(10, "LTE45", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        insertSteps(10, "GTH315", false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                    }
                } else if (allianceColor.equals("Blue")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        insertSteps(10, "FWE-24", false, false, 0, 0, 0, 0, 0, 0, 0.6, mintCurrentStep + 1);
                        insertSteps(10, "FWE17", false, false, 0, 0, 0, 0, 0, 0, 0.35, mintCurrentStep + 1);
                        //insertSteps(10, "RTE45", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        insertSteps(10, "GTH90", false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                        insertSteps(10, "FWE4", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        //insertSteps(10, "LTE45", false, false, 0, 0, 0, 0, 0, 0, 0.5, mintCurrentStep + 1);
                        insertSteps(10, "GTH45", false, false, 0, 0, 0, 0, 0, 0, 0.3, mintCurrentStep + 1);
                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        insertSteps(10, "FWE-24", false, false, 0, 0, 0, 0, 0, 0, 0.6, mintCurrentStep + 1);
                        insertSteps(10, "FWE18", false, false, 0, 0, 0, 0, 0, 0, 0.35, mintCurrentStep + 1);
                    }
                }
*/

                if (allianceColor.equals("Red")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME + 90, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 7, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOff);

                        blnColourOK = true;
                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 90, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOn);
                        blnColourOK = true;
                    } else if (mColour == Constants.BeaconColours.BEACON_RED)
                    {
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOff);
                        insertSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0, mintCurrentStep + 1);
                        insertSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47, mintCurrentStep + 1);
                        blnColourOK = false;
                    } else if (mColour == Constants.BeaconColours.BEACON_BLUE)
                    {
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOn);
                        insertSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0, mintCurrentStep + 1);
                        insertSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47, mintCurrentStep + 1);
                        blnColourOK = false;
                    }
                } else if (allianceColor.equals("Blue")) {
                    if (mColour == Constants.BeaconColours.BEACON_BLUE_RED) {    //means red is to the right
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME , SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME + 90, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOff);
                    } else if (mColour == Constants.BeaconColours.BEACON_RED_BLUE) {
                        moveServo(servoBeaconRight, SERVOBEACONRIGHT_HOME + 90, SERVOBEACONRIGHT_MIN_RANGE, SERVOBEACONRIGHT_MAX_RANGE);
                        moveServo(servoBeaconLeft, SERVOBEACONLEFT_HOME, SERVOBEACONLEFT_MIN_RANGE, SERVOBEACONLEFT_MAX_RANGE);
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOn);
                    }  else if (mColour == Constants.BeaconColours.BEACON_RED)
                    {
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOff);
                        insertSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0, mintCurrentStep + 1);
                        insertSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47, mintCurrentStep + 1);
                        blnColourOK = false;
                    } else if (mColour == Constants.BeaconColours.BEACON_BLUE)
                    {
                        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED1_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, LedOn);
                        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, LedOff);   //turn LED ON
                        dim.setDigitalChannelState(RED2_LED_CHANNEL, LedOff);
                        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, LedOn);
                        insertSteps(5,  "BCL",  false, false,   0,    0,    0,    0,    0,    0,    0, mintCurrentStep + 1);
                        insertSteps(3, "GTE180",false, false,   0,    0,    0,    0,    0,    0,    0.47, mintCurrentStep + 1);
                        blnColourOK = false;
                    }
                }
                if (blnColourOK) {
                    mint5291LEDStatus = LEDState.STATE_BEACON;
                    readRangeSensors();  // This takes 100ms so use it wisely
                    dblMaxDistance = Math.max(Math.abs(mdblRangeSensor1), Math.abs(mdblRangeSensor2));  //get the maxiumum distance from wall
                    if (dblMaxDistance > 50) {
                        readRangeSensors();  // This takes 100ms so use it wisely
                        dblMaxDistance = Math.max(Math.abs(mdblRangeSensor1), Math.abs(mdblRangeSensor2));  //get the maxiumum distance from wall
                    }
                    if (dblMaxDistance > 50) {
                        readRangeSensors();  // This takes 100ms so use it wisely
                        dblMaxDistance = Math.min(Math.abs(mdblRangeSensor1), Math.abs(mdblRangeSensor2)) + 5;  //get the maxiumum distance from wall
                    }

                    if (!(dblMaxDistance == 0)) {
                        insertSteps(5, "FWE-12", false, false, 0, 0, 0, 0, 0, 0, 0.4, mintCurrentStep + 1);
                        insertSteps(5, "FWE" + (dblMaxDistance / 2.54), false, true, 0, 0, 0, 0, 0, 0, 0.4, mintCurrentStep + 1);
                    }
                } else {
                    mint5291LEDStatus = LEDState.STATE_ERROR;
                }
                if (debug >= 2) {
                    fileLogger.writeEvent("AttackBeacon()", "Returned mColour " + mColour);
                    fileLogger.writeEvent("AttackBeacon()", "dblMaxDistance " + dblMaxDistance);
                    Log.d("AttackBeacon()", "Returned mColour " + mColour);
                    Log.d("AttackBeacon()", "dblMaxDistance " + dblMaxDistance);
                }

                mintCurrentAttackBeaconState = stepState.STATE_COMPLETE;
            }
            break;
        }
    }

    private void DelayStep ()
    {
        switch (mintCurrentDelayState) {
            case STATE_INIT: {
                mintStepDelay = Integer.parseInt(mdblRobotCommand.substring(3));
                mintCurrentDelayState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("DelayStep()", "Init Delay Time    " + mintStepDelay);
                    Log.d("DelayStep()", "Init Delay Time    " + mintStepDelay);
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (mStateTime.milliseconds() >= mintStepDelay) {
                    if (debug >= 1) {
                        fileLogger.writeEvent("DelayStep()", "Complete         ");
                        Log.d("DelayStep()", "Complete         ");
                    }
                    mintCurrentDelayState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }

    private void SFLineFind5291 ()
    {
        double dblDistanceToEndLeft1;
        double dblDistanceToEndLeft2;
        double dblDistanceToEndRight1;
        double dblDistanceToEndRight2;
        int intLeft1MotorEncoderPosition;
        int intLeft2MotorEncoderPosition;
        int intRight1MotorEncoderPosition;
        int intRight2MotorEncoderPosition;
        boolean blnFoundLine = false;

        switch (mint5291CurrentLineFindState) {
            case STATE_INIT: {
                mblnNextStepLastPos = false;

                if (debug >= 2) {
                    fileLogger.writeEvent("runningDriveHeadingStep", "Init");
                    Log.d("runningDriveHeadingStep", "Init");
                }
                //set motors to run using encoders, we will not run to a position rather will try and measure it based on encoder counts
                robotDrive.leftMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.leftMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                robotDrive.rightMotor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                mblnDisableVisionProcessing = true;  //disable vision processing

                //this is the maximum distance the robot will move before aborting looking for the line
                mdblStepDistance = mdblRobotParm1;

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveHeadingStep", "mdblStepDistance   :- " + mdblStepDistance);
                    Log.d("runningDriveHeadingStep", "mdblStepDistance   :- " + mdblStepDistance);
                }
                // Determine new target position

                mintStartPositionLeft1 = robotDrive.leftMotor1.getCurrentPosition();
                mintStartPositionLeft2 = robotDrive.leftMotor2.getCurrentPosition();
                mintStartPositionRight1 = robotDrive.rightMotor1.getCurrentPosition();
                mintStartPositionRight2 = robotDrive.rightMotor2.getCurrentPosition();

                mintStepLeftTarget1 = mintStartPositionLeft1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepLeftTarget2 = mintStartPositionLeft2 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget1 = mintStartPositionRight1 + (int) (mdblStepDistance * COUNTS_PER_INCH);
                mintStepRightTarget2 = mintStartPositionRight2 + (int) (mdblStepDistance * COUNTS_PER_INCH);

                if (debug >= 2)
                {
                    fileLogger.writeEvent("runningDriveHeadingStep", "mStepLeftTarget1 :- " + mintStepLeftTarget1 +  " mStepLeftTarget2 :- " + mintStepLeftTarget2);
                    fileLogger.writeEvent("runningDriveHeadingStep", "mStepRightTarget1:- " + mintStepRightTarget1 + " mStepRightTarget2:- " + mintStepRightTarget2);
                    Log.d("runningDriveHeadingStep", "mStepLeftTarget1 :- " + mintStepLeftTarget1 +  " mStepLeftTarget2 :- " + mintStepLeftTarget2);
                    Log.d("runningDriveHeadingStep", "mStepRightTarget1:- " + mintStepRightTarget1 + " mStepRightTarget2:- " + mintStepRightTarget2);
                }

                mint5291CurrentLineFindState = stepState.STATE_RUNNING;

                setDriveMotorPower(Math.abs(mdblStepSpeed));

            }
            break;
            case STATE_RUNNING:
            {

                intLeft1MotorEncoderPosition = robotDrive.leftMotor1.getCurrentPosition();
                intLeft2MotorEncoderPosition = robotDrive.leftMotor2.getCurrentPosition();
                intRight1MotorEncoderPosition = robotDrive.rightMotor1.getCurrentPosition();
                intRight2MotorEncoderPosition = robotDrive.rightMotor2.getCurrentPosition();

                //determine how close to target we are
                dblDistanceToEndLeft1 = (mintStepLeftTarget1 - intLeft1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndLeft2 = (mintStepLeftTarget2 - intLeft2MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight1 = (mintStepRightTarget1 - intRight1MotorEncoderPosition) / COUNTS_PER_INCH;
                dblDistanceToEndRight2 = (mintStepRightTarget2 - intRight2MotorEncoderPosition) / COUNTS_PER_INCH;

                mdblInputLineSensor1 = LineSensor1.getVoltage();    //  Read the input pin
                mdblInputLineSensor2 = LineSensor2.getVoltage();    //  Read the input pin
                mdblInputLineSensor3 = LineSensor3.getVoltage();    //  Read the input pin

                if ((mdblInputLineSensor1 < mdblWhiteThreshold) || (mdblInputLineSensor2 < mdblWhiteThreshold) || (mdblInputLineSensor3 < mdblWhiteThreshold))
                {
                    blnFoundLine = true;
                }    //stop the motors we are complete

                if ( (blnFoundLine)  ||
                        (( dblDistanceToEndLeft1 <= 1 ) && ( dblDistanceToEndLeft2 <= 1 )) ||
                        (( dblDistanceToEndRight1 <= 1 ) && ( dblDistanceToEndRight2 <= 1 ))  )
                {
                    mblnNextStepLastPos = false;
                    setDriveMotorPower(0);
                    mint5291CurrentLineFindState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }

    private void FlickerShooter5291 ()
    {
        switch (mint5291CurrentShootParticleState) {
            case STATE_INIT: {
                mint5291CurrentShootParticleState = stepState.STATE_RUNNING;
                if (debug >= 2) {
                    fileLogger.writeEvent("ShootBallStep()", "Init");
                    Log.d("ShootBallStep()", "Init");
                }
            }
            break;
            case STATE_RUNNING:
            {
                if (mStateTime.milliseconds() >= (int)mdblRobotParm1) {
                    //stop shooting we are complete
                    armDrive.sweeper.setPower(0);
                    armDrive.flicker.setPower(0);
                    if (debug >= 1) {
                        fileLogger.writeEvent("ShootBallStep()", "Complete         ");
                        Log.d("ShootBallStep()", "Complete         ");
                    }
                    mint5291CurrentShootParticleState = stepState.STATE_COMPLETE;
                } else {
                    //start shooting
                    armDrive.sweeper.setPower(1);
                    armDrive.flicker.setPower(1);
                }
            }
            break;
        }
    }

    //----------------------------------------------------------------------------------------------
    //code added 23/02/16 active the flicker
    //
    //
    //----------------------------------------------------------------------------------------------
    private void FlickerShooter11231()
    {
        switch (mint11231CurrentShootParticleState) {
            case STATE_INIT: {
                armDrive.flicker.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                armDrive.flicker.setPower(0);
                mint11231CurrentShootParticleState = stepState.STATE_RUNNING;
                {
                    fileLogger.writeEvent("setFlickerShooter()","INIT");
                    Log.d("setFlickerShooter()","INIT");
                }
            }
            break;
            case STATE_RUNNING: {
                if (mStateTime.seconds() <= mdblRobotParm1) {
                    armDrive.flicker.setPower(.4);
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("setFlickerShooter()","FLICKER ON");
                        fileLogger.writeEvent("setFlickerShooter()","RUNNING");
                        Log.d("setFlickerShooter()","FLICKER ON");
                        Log.d("setFlickerShooter()","RUNNING");
                    }
                } else {
                    if (debug >= 3)
                    {
                        fileLogger.writeEvent("setFlickerShooter()","FLICKER OFF");
                        fileLogger.writeEvent("setFlickerShooter()","Complete");
                        Log.d("setFlickerShooter()","FLICKER OFF");
                        Log.d("setFlickerShooter()","Complete");
                    }
                    armDrive.flicker.setPower(0);
                    mint11231CurrentShootParticleState = stepState.STATE_COMPLETE;
                }
            }
            break;
        }
    }

    private void LedState (boolean g1, boolean r1, boolean b1, boolean g2, boolean r2, boolean b2) {

        dim.setDigitalChannelState(GREEN1_LED_CHANNEL, g1);   //turn LED ON
        dim.setDigitalChannelState(RED1_LED_CHANNEL, r1);
        dim.setDigitalChannelState(BLUE1_LED_CHANNEL, b1);
        dim.setDigitalChannelState(GREEN2_LED_CHANNEL, g2);   //turn LED ON
        dim.setDigitalChannelState(RED2_LED_CHANNEL, r2);
        dim.setDigitalChannelState(BLUE2_LED_CHANNEL, b2);

    }

    private String getAngle(int angle1, int angle2)
    {
        if (debug >= 2)
        {
            fileLogger.writeEvent(TAG, "Getangle - Current Angle1:= " + angle1 + " Desired Angle2:= " + angle2);
            Log.d(TAG, "Getangle - Current Angle1:= " + angle1 + " Desired Angle2:= " + angle2);

        }

        switch (angle1)
        {
            case 0:
                switch (angle2)
                {
                    case 45:
                        return "RTE45";
                    case 90:
                        return "RTE90";
                    case 135:
                        return "RTE135";
                    case 180:
                        return "RTE180";
                    case 225:
                        return "LTE135";
                    case 270:
                        return "LTE90";
                    case 315:
                        return "LTE45";
                }
                break;
            case 45:
                switch (angle2)
                {
                    case 0:
                        return "LTE45";
                    case 90:
                        return "RTE45";
                    case 135:
                        return "RTE90";
                    case 180:
                        return "RTE135";
                    case 225:
                        return "RTE180";
                    case 270:
                        return "LTE135";
                    case 315:
                        return "LTE90";
                }
                break;
            case 90:
                switch (angle2)
                {
                    case 0:
                        return "LTE90";
                    case 45:
                        return "LTE45";
                    case 135:
                        return "RTE45";
                    case 180:
                        return "RTE90";
                    case 225:
                        return "RTE135";
                    case 270:
                        return "RTE180";
                    case 315:
                        return "LTE135";
                }
                break;
            case 135:
                switch (angle2)
                {
                    case 0:
                        return "LTE135";
                    case 45:
                        return "LTE90";
                    case 90:
                        return "LTE45";
                    case 180:
                        return "RTE45";
                    case 225:
                        return "RTE90";
                    case 270:
                        return "RTE135";
                    case 315:
                        return "RTE180";
                }
                break;
            case 180:
                switch (angle2)
                {
                    case 0:
                        return "LTE180";
                    case 45:
                        return "LTE135";
                    case 90:
                        return "LTE90";
                    case 135:
                        return "LTE45";
                    case 225:
                        return "RTE45";
                    case 270:
                        return "RTE90";
                    case 315:
                        return "RTE135";
                }
                break;
            case 225:
                switch (angle2)
                {
                    case 0:
                        return "RTE135";
                    case 45:
                        return "LTE180";
                    case 90:
                        return "LTE135";
                    case 135:
                        return "LTE90";
                    case 180:
                        return "LTE45";
                    case 270:
                        return "RTE45";
                    case 315:
                        return "RTE90";
                }
                break;
            case 270:
                switch (angle2)
                {
                    case 0:
                        return "RTE90";
                    case 45:
                        return "RTE135";
                    case 90:
                        return "LTE180";
                    case 135:
                        return "LTE135";
                    case 180:
                        return "LTE90";
                    case 225:
                        return "LTE45";
                    case 315:
                        return "RTE45";
                }
                break;
            case 315:
                switch (angle2)
                {
                    case 0:
                        return "RTE45";
                    case 45:
                        return "RTE90";
                    case 90:
                        return "RTE135";
                    case 135:
                        return "LTE180";
                    case 180:
                        return "LTE135";
                    case 225:
                        return "LTE90";
                    case 270:
                        return "LTE45";
                }
                break;
        }
        return "ERROR";
    }

    private String newAngleDirection (int currentDirection, int newDirection)
    {
        if (currentDirection < newDirection)
            return "LTE" + (newDirection - currentDirection);
        else
            return "RTE" + (currentDirection - newDirection);
    }

    private double teamAngleAdjust ( double angle ) {

        if (debug >= 2)
        {
            fileLogger.writeEvent("teamAngleAdjust", "teamAngleAdjust - angle " + angle + " allianceColor " + allianceColor);
            Log.d("teamAngleAdjust", "teamAngleAdjust - angle " + angle + " allianceColor " + allianceColor);
        }

        if (allianceColor.equals("Red")) {
            //angle = angle + 90;  if starting against the wall
            //angle = angle + 225; if starting at 45 to the wall facing the beacon
            angle = angle + 225;
            if (angle > 360) {
                angle = angle - 360;
            }
            if (debug >= 2)
            {
                fileLogger.writeEvent("teamAngleAdjust", "In RED Angle " + angle);
                Log.d("teamAngleAdjust", "In RED Angle " + angle);
            }

        } else
        if (allianceColor.equals("Blue")) {
            angle = angle - 180;
            if (angle < 0) {
                angle = angle + 360;
            }
        }
        return angle;
    }

    private String newAngleDirectionGyro (int currentDirection, int newDirection)
    {
        int intAngle1;

        //calculate the smallest angle
        if (currentDirection < newDirection) {
            intAngle1 = (newDirection - currentDirection);
            if (intAngle1 > 180)
            {
                intAngle1 = (currentDirection + 360 - newDirection);
                return "LTE" + intAngle1;
            }
            return "RTE" + intAngle1;
        }
        else
        {
            intAngle1 = (currentDirection - newDirection);
            if (intAngle1 > 180)
            {
                intAngle1 = (newDirection + 360 - currentDirection);
                return "RTE" + intAngle1;
            }
            return "LTE" + intAngle1;
        }
    }

    private String format(OpenGLMatrix transformationMatrix) {
        return transformationMatrix.formatAsTransform();
    }

    //set the drive motors power, both left and right
    private void setDriveMotorPower (double power) {
        setDriveRightMotorPower(power);
        setDriveLeftMotorPower(power);
    }

    //set the right drive motors power
    private void setDriveRightMotorPower (double power) {
        robotDrive.rightMotor1.setPower(power);
        robotDrive.rightMotor2.setPower(power);
    }

    //set the left motors drive power
    private void setDriveLeftMotorPower (double power) {
        robotDrive.leftMotor1.setPower(power);
        robotDrive.leftMotor2.setPower(power);
    }


    public boolean moveServo (Servo Servo, double Position, double RangeMin, double RangeMax ) {
        boolean OKToMove = true;

        //set right position
        if ((Range.scale(Position, 0, 180, 0, 1) < RangeMin ) || (Range.scale(Position, 0, 180, 0, 1) > RangeMax )) {
            return false;
        }

        Servo.setPosition(Range.scale(Position, 0, 180, 0, 1));

        return true;
    }

    /**
     * Converts a reading of the optical sensor into centimeters. This computation
     * could be adjusted by altering the numeric parameters, or by providing an alternate
     * calculation in a subclass.
     */
    double cmFromOptical(int opticalReading)
    {
        double pParam = -1.02001;
        double qParam = 0.00311326;
        double rParam = -8.39366;
        int    sParam = 10;

        if (opticalReading < sParam)
            return 0;
        else
            return pParam * Math.log(qParam * (rParam + opticalReading));
    }

    int cmUltrasonic(int rawUS)
    {
        return rawUS;
    }

    double cmOptical(int rawOptical)
    {
        return cmFromOptical(rawOptical);
    }

    public double getDistance(int rawUS, int rawOptical, DistanceUnit unit)
    {
        double cmOptical = cmOptical(rawOptical);
        double cm        = cmOptical > 0 ? cmOptical : cmUltrasonic(rawUS);
        return unit.fromUnit(DistanceUnit.CM, cm);
    }

    public void readRangeSensors()
    {
        //adds 100ms to scan time, try use this as little as possible
        range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        mdblRangeSensor1 = getDistance(range1Cache[0] & 0xFF, range1Cache[1] & 0xFF, DistanceUnit.CM);
        mdblRangeSensor2 = getDistance(range2Cache[0] & 0xFF, range2Cache[1] & 0xFF, DistanceUnit.CM);
        if (debug >= 2) {
            fileLogger.writeEvent("readRangeSensors()", "mdblRangeSensor1 " + mdblRangeSensor1 + ",mdblRangeSensor2 " + mdblRangeSensor2);
            Log.d("readRangeSensors()", "mdblRangeSensor1 " + mdblRangeSensor1 + ",mdblRangeSensor2 " + mdblRangeSensor2);
        }
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
     * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     *          +ve error means the robot should turn LEFT (CCW) to reduce error.
     */
    public double getDriveError(double targetAngle) {

        double robotError;

        if (debug >= 2) {
            fileLogger.writeEvent("getDriveError()", "targetAngle " + targetAngle);
            Log.d("getDriveError()", "targetAngle " + targetAngle);
        }

        // calculate error in -179 to +180 range  (
        robotError = targetAngle - teamAngleAdjust(gyro.getHeading());
        if (debug >= 3) {
            fileLogger.writeEvent("getDriveError()", "robotError1 " + robotError + ", gyro.getHeading() " + gyro.getHeading() + " teamAngleAdjust(gyro.getHeading() "  + teamAngleAdjust(gyro.getHeading()));
            Log.d("getDriveError()", "robotError1 " + robotError);
        }
        if (robotError > 180)
            robotError -= 360;
        if (robotError <= -180)
            robotError += 360;

        if (debug >= 2) {
            fileLogger.writeEvent("getDriveError()", "robotError2 " + robotError);
            Log.d("getDriveError()", "robotError2 " + robotError);
        }
        return -robotError;
    }

    /**
     * returns desired steering force.  +/- 1 range.  +ve = steer left
     * @param error   Error angle in robot relative degrees
     * @param PCoeff  Proportional Gain Coefficient
     * @return
     */
    public double getDriveSteer(double error, double PCoeff) {
        return Range.clip(error * PCoeff, -1, 1);
    }


}
