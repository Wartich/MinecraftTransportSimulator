package minecrafttransportsimulator.baseclasses;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Utility class for converting between MTS types and JBullet/vecmath types.
 * MTS uses double precision, JBullet uses float precision.
 * 
 * @author clanka
 */
public class PhysicsConversions {
    
    /**
     * Converts a Point3D to a Vector3f.
     * Note: precision loss from double to float.
     */
    public static Vector3f toVector3f(Point3D point) {
        return new Vector3f((float) point.x, (float) point.y, (float) point.z);
    }
    
    /**
     * Converts a Point3D to a Vector3f, storing result in the provided vector.
     * Note: precision loss from double to float.
     */
    public static void toVector3f(Point3D point, Vector3f out) {
        out.set((float) point.x, (float) point.y, (float) point.z);
    }
    
    /**
     * Converts a Vector3f to a Point3D.
     */
    public static Point3D toPoint3D(Vector3f vector) {
        return new Point3D(vector.x, vector.y, vector.z);
    }
    
    /**
     * Converts a Vector3f to a Point3D, storing result in the provided point.
     */
    public static void toPoint3D(Vector3f vector, Point3D out) {
        out.set(vector.x, vector.y, vector.z);
    }
    
    /**
     * Converts a RotationMatrix to a Matrix3f.
     * Note: precision loss from double to float.
     */
    public static Matrix3f toMatrix3f(RotationMatrix rotation) {
        Matrix3f matrix = new Matrix3f();
        matrix.m00 = (float) rotation.m00;
        matrix.m01 = (float) rotation.m01;
        matrix.m02 = (float) rotation.m02;
        matrix.m10 = (float) rotation.m10;
        matrix.m11 = (float) rotation.m11;
        matrix.m12 = (float) rotation.m12;
        matrix.m20 = (float) rotation.m20;
        matrix.m21 = (float) rotation.m21;
        matrix.m22 = (float) rotation.m22;
        return matrix;
    }
    
    /**
     * Converts a RotationMatrix to a Matrix3f, storing result in the provided matrix.
     * Note: precision loss from double to float.
     */
    public static void toMatrix3f(RotationMatrix rotation, Matrix3f out) {
        out.m00 = (float) rotation.m00;
        out.m01 = (float) rotation.m01;
        out.m02 = (float) rotation.m02;
        out.m10 = (float) rotation.m10;
        out.m11 = (float) rotation.m11;
        out.m12 = (float) rotation.m12;
        out.m20 = (float) rotation.m20;
        out.m21 = (float) rotation.m21;
        out.m22 = (float) rotation.m22;
    }
    
    /**
     * Converts a Matrix3f to a RotationMatrix.
     */
    public static RotationMatrix toRotationMatrix(Matrix3f matrix) {
        RotationMatrix rotation = new RotationMatrix();
        rotation.m00 = matrix.m00;
        rotation.m01 = matrix.m01;
        rotation.m02 = matrix.m02;
        rotation.m10 = matrix.m10;
        rotation.m11 = matrix.m11;
        rotation.m12 = matrix.m12;
        rotation.m20 = matrix.m20;
        rotation.m21 = matrix.m21;
        rotation.m22 = matrix.m22;
        rotation.bypassAngles(); // Don't try to convert back to angles
        return rotation;
    }
    
    /**
     * Converts a Matrix3f to a RotationMatrix, storing result in the provided rotation.
     */
    public static void toRotationMatrix(Matrix3f matrix, RotationMatrix out) {
        out.m00 = matrix.m00;
        out.m01 = matrix.m01;
        out.m02 = matrix.m02;
        out.m10 = matrix.m10;
        out.m11 = matrix.m11;
        out.m12 = matrix.m12;
        out.m20 = matrix.m20;
        out.m21 = matrix.m21;
        out.m22 = matrix.m22;
        out.bypassAngles(); // Don't try to convert back to angles
    }
    
    /**
     * Converts a RotationMatrix to a Quat4f quaternion.
     * Note: precision loss from double to float.
     */
    public static Quat4f toQuat4f(RotationMatrix rotation) {
        // Convert rotation matrix to quaternion
        // Using the algorithm from: https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
        Quat4f quat = new Quat4f();
        
        double trace = rotation.m00 + rotation.m11 + rotation.m22;
        
        if (trace > 0) {
            double s = 0.5 / Math.sqrt(trace + 1.0);
            quat.w = (float) (0.25 / s);
            quat.x = (float) ((rotation.m21 - rotation.m12) * s);
            quat.y = (float) ((rotation.m02 - rotation.m20) * s);
            quat.z = (float) ((rotation.m10 - rotation.m01) * s);
        } else if (rotation.m00 > rotation.m11 && rotation.m00 > rotation.m22) {
            double s = 2.0 * Math.sqrt(1.0 + rotation.m00 - rotation.m11 - rotation.m22);
            quat.w = (float) ((rotation.m21 - rotation.m12) / s);
            quat.x = (float) (0.25 * s);
            quat.y = (float) ((rotation.m01 + rotation.m10) / s);
            quat.z = (float) ((rotation.m02 + rotation.m20) / s);
        } else if (rotation.m11 > rotation.m22) {
            double s = 2.0 * Math.sqrt(1.0 + rotation.m11 - rotation.m00 - rotation.m22);
            quat.w = (float) ((rotation.m02 - rotation.m20) / s);
            quat.x = (float) ((rotation.m01 + rotation.m10) / s);
            quat.y = (float) (0.25 * s);
            quat.z = (float) ((rotation.m12 + rotation.m21) / s);
        } else {
            double s = 2.0 * Math.sqrt(1.0 + rotation.m22 - rotation.m00 - rotation.m11);
            quat.w = (float) ((rotation.m10 - rotation.m01) / s);
            quat.x = (float) ((rotation.m02 + rotation.m20) / s);
            quat.y = (float) ((rotation.m12 + rotation.m21) / s);
            quat.z = (float) (0.25 * s);
        }
        
        return quat;
    }
    
    /**
     * Converts a Quat4f quaternion to a RotationMatrix.
     */
    public static RotationMatrix toRotationMatrix(Quat4f quat) {
        RotationMatrix rotation = new RotationMatrix();
        
        // Convert quaternion to rotation matrix
        double xx = quat.x * quat.x;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double xw = quat.x * quat.w;
        
        double yy = quat.y * quat.y;
        double yz = quat.y * quat.z;
        double yw = quat.y * quat.w;
        
        double zz = quat.z * quat.z;
        double zw = quat.z * quat.w;
        
        rotation.m00 = 1 - 2 * (yy + zz);
        rotation.m01 = 2 * (xy - zw);
        rotation.m02 = 2 * (xz + yw);
        
        rotation.m10 = 2 * (xy + zw);
        rotation.m11 = 1 - 2 * (xx + zz);
        rotation.m12 = 2 * (yz - xw);
        
        rotation.m20 = 2 * (xz - yw);
        rotation.m21 = 2 * (yz + xw);
        rotation.m22 = 1 - 2 * (xx + yy);
        
        rotation.bypassAngles(); // Don't try to convert back to angles
        return rotation;
    }
    
    /**
     * Converts a Quat4f quaternion to a RotationMatrix, storing result in the provided rotation.
     */
    public static void toRotationMatrix(Quat4f quat, RotationMatrix out) {
        // Convert quaternion to rotation matrix
        double xx = quat.x * quat.x;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double xw = quat.x * quat.w;
        
        double yy = quat.y * quat.y;
        double yz = quat.y * quat.z;
        double yw = quat.y * quat.w;
        
        double zz = quat.z * quat.z;
        double zw = quat.z * quat.w;
        
        out.m00 = 1 - 2 * (yy + zz);
        out.m01 = 2 * (xy - zw);
        out.m02 = 2 * (xz + yw);
        
        out.m10 = 2 * (xy + zw);
        out.m11 = 1 - 2 * (xx + zz);
        out.m12 = 2 * (yz - xw);
        
        out.m20 = 2 * (xz - yw);
        out.m21 = 2 * (yz + xw);
        out.m22 = 1 - 2 * (xx + yy);
        
        out.bypassAngles(); // Don't try to convert back to angles
    }
    
    // Convenience aliases for clearer code
    
    /**
     * Alias for toVector3f(Point3D, Vector3f).
     */
    public static void point3DToVector3f(Point3D point, Vector3f out) {
        toVector3f(point, out);
    }
    
    /**
     * Alias for toPoint3D(Vector3f, Point3D).
     */
    public static void vector3fToPoint3D(Vector3f vector, Point3D out) {
        toPoint3D(vector, out);
    }
    
    /**
     * Alias for toMatrix3f(RotationMatrix, Matrix3f).
     */
    public static void rotationMatrixToMatrix3f(RotationMatrix rotation, Matrix3f out) {
        toMatrix3f(rotation, out);
    }
    
    /**
     * Alias for toRotationMatrix(Matrix3f, RotationMatrix).
     */
    public static void matrix3fToRotationMatrix(Matrix3f matrix, RotationMatrix out) {
        toRotationMatrix(matrix, out);
    }
}
