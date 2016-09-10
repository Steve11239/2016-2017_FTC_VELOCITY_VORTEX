package club.towr5291.opmodes;

/**
 * Created by ianhaden on 2/09/16.
 * Define a "PathSegment" object, used for building a path for the robot to follow.
 */
public class LibraryStateSegAuto {

    public double mRobotTimeOut;        //how much time is allowed for the step to complete
    public double mRobotDistance;       //how far to move in inches
    public double mRobotSpeed;          //how fast to move -1 to 1
    public String mRobotDirection;      //what angle to move in

    // Constructor
    public LibraryStateSegAuto(double timeout, String robotDirection, double robotDistance, double robotSpeed)
    {
        mRobotTimeOut = timeout;
        mRobotDirection = robotDirection;
        mRobotDistance = robotDistance;
        mRobotSpeed = robotSpeed;
    }


}
