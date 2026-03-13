/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.math;

import java.util.ArrayList;
import java.util.List;
import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.UsedFromLua;
import zombie.core.math.interpolators.LerpType;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.StringUtils;

@UsedFromLua
public final class PZMath {
    public static final float PI = (float)java.lang.Math.PI;
    public static final float PI2 = (float)java.lang.Math.PI * 2;
    public static final float halfPI = 1.5707964f;
    public static final float degToRads = (float)java.lang.Math.PI / 180;
    public static final float radToDegs = 57.295776f;
    public static final long microsToNanos = 1000L;
    public static final long millisToMicros = 1000L;
    public static final long secondsToMillis = 1000L;
    public static long secondsToNanos = 1000000000L;

    public static float almostUnitIdentity(float x) {
        return x * x * (2.0f - x);
    }

    public static float almostIdentity(float x, float m, float n) {
        if (x > m) {
            return x;
        }
        float a = 2.0f * n - m;
        float b = 2.0f * m - 3.0f * n;
        float t = x / m;
        return (a * t + b) * t * t + n;
    }

    public static float gain(float x, float k) {
        float a = (float)(0.5 * java.lang.Math.pow(2.0f * (x < 0.5f ? x : 1.0f - x), k));
        return x < 0.5f ? a : 1.0f - a;
    }

    public static float clamp(float val, float min, float max) {
        if (val < min) {
            return min;
        }
        if (val > max) {
            return max;
        }
        return val;
    }

    public static long clamp(long val, long min, long max) {
        if (val < min) {
            return min;
        }
        if (val > max) {
            return max;
        }
        return val;
    }

    public static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        }
        if (val > max) {
            return max;
        }
        return val;
    }

    public static double clamp(double val, double min, double max) {
        if (val < min) {
            return min;
        }
        if (val > max) {
            return max;
        }
        return val;
    }

    public static float clampFloat(float val, float min, float max) {
        return PZMath.clamp(val, min, max);
    }

    public static float clamp_01(float val) {
        return PZMath.clamp(val, 0.0f, 1.0f);
    }

    public static double clampDouble_01(double val) {
        return PZMath.clamp(val, 0.0, 1.0);
    }

    public static Quaternion setFromAxisAngle(float ax, float ay, float az, float angleRadians, Quaternion result) {
        result.x = ax;
        result.y = ay;
        result.z = az;
        float n = (float)java.lang.Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);
        float s = (float)(java.lang.Math.sin(0.5 * (double)angleRadians) / (double)n);
        result.x *= s;
        result.y *= s;
        result.z *= s;
        result.w = (float)java.lang.Math.cos(0.5 * (double)angleRadians);
        return result;
    }

    public static float lerp(float src, float dest, float alpha) {
        return src + (dest - src) * alpha;
    }

    public static float lerp(float src, float dest, float alpha, LerpType lerpType) {
        return switch (lerpType) {
            case LerpType.Linear -> PZMath.lerp(src, dest, alpha);
            case LerpType.EaseOutQuad -> PZMath.lerp(src, dest, PZMath.lerpFunc_EaseOutQuad(alpha));
            case LerpType.EaseInQuad -> PZMath.lerp(src, dest, PZMath.lerpFunc_EaseInQuad(alpha));
            case LerpType.EaseOutInQuad -> PZMath.lerp(src, dest, PZMath.lerpFunc_EaseOutInQuad(alpha));
            default -> throw new UnsupportedOperationException();
        };
    }

    public static float lerpAngle(float src, float dest, float alpha) {
        float diff = PZMath.getClosestAngle(src, dest);
        float lerped = src + alpha * diff;
        return PZMath.wrap(lerped, (float)(-java.lang.Math.PI), (float)java.lang.Math.PI);
    }

    public static Vector3f lerp(Vector3f out, Vector3f a, Vector3f b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        return out;
    }

    public static Vector3 lerp(Vector3 out, Vector3 a, Vector3 b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        return out;
    }

    public static Vector2 lerp(Vector2 out, Vector2 a, Vector2 b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        return out;
    }

    public static float c_lerp(float src, float dest, float alpha) {
        float t2 = (float)(1.0 - java.lang.Math.cos(alpha * (float)java.lang.Math.PI)) / 2.0f;
        return src * (1.0f - t2) + dest * t2;
    }

    public static Quaternion slerp(Quaternion result, Quaternion from, Quaternion to, float alpha) {
        double dot = from.x * to.x + from.y * to.y + from.z * to.z + from.w * to.w;
        double absDot = dot < 0.0 ? -dot : dot;
        double scale0 = 1.0f - alpha;
        double scale1 = alpha;
        if (1.0 - absDot > 0.1) {
            double angle = Math.acos(absDot);
            double sinAngle = Math.sin(angle);
            double invSinAngle = 1.0 / sinAngle;
            scale0 = Math.sin(angle * (1.0 - (double)alpha)) * invSinAngle;
            scale1 = Math.sin(angle * (double)alpha) * invSinAngle;
        }
        if (dot < 0.0) {
            scale1 = -scale1;
        }
        result.set((float)(scale0 * (double)from.x + scale1 * (double)to.x), (float)(scale0 * (double)from.y + scale1 * (double)to.y), (float)(scale0 * (double)from.z + scale1 * (double)to.z), (float)(scale0 * (double)from.w + scale1 * (double)to.w));
        return result;
    }

    public static float sqrt(float val) {
        return Math.sqrt(val);
    }

    public static float lerpFunc_EaseOutQuad(float x) {
        return x * x;
    }

    public static float lerpFunc_EaseInQuad(float x) {
        float revX = 1.0f - x;
        return 1.0f - revX * revX;
    }

    public static float lerpFunc_EaseOutInQuad(float x) {
        if (x < 0.5f) {
            return PZMath.lerpFunc_EaseOutQuad(x) * 2.0f;
        }
        return 0.5f + PZMath.lerpFunc_EaseInQuad(2.0f * x - 1.0f) / 2.0f;
    }

    public static double tryParseDouble(String varStr, double defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(varStr.trim());
        }
        catch (NumberFormatException ignored) {
            return defaultVal;
        }
    }

    public static float tryParseFloat(String varStr, float defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        }
        try {
            return Float.parseFloat(varStr.trim());
        }
        catch (NumberFormatException ignored) {
            return defaultVal;
        }
    }

    public static boolean canParseFloat(String varStr) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return false;
        }
        try {
            Float.parseFloat(varStr.trim());
            return true;
        }
        catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static int tryParseInt(String varStr, int defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(varStr.trim());
        }
        catch (NumberFormatException ignored) {
            return defaultVal;
        }
    }

    public static float pow(float a, float b) {
        return (float)java.lang.Math.pow(a, b);
    }

    public static float degToRad(float degrees) {
        return (float)java.lang.Math.PI / 180 * degrees;
    }

    public static float radToDeg(float radians) {
        return 57.295776f * radians;
    }

    public static float getClosestAngle(float radsA, float radsB) {
        float angleA = PZMath.wrap(radsA, (float)java.lang.Math.PI * 2);
        float angleB = PZMath.wrap(radsB, (float)java.lang.Math.PI * 2);
        float diff = angleB - angleA;
        return PZMath.wrap(diff, (float)(-java.lang.Math.PI), (float)java.lang.Math.PI);
    }

    public static float getClosestAngleDegrees(float degsA, float degsB) {
        float radsA = PZMath.degToRad(degsA);
        float radsB = PZMath.degToRad(degsB);
        float closestAngleRads = PZMath.getClosestAngle(radsA, radsB);
        return PZMath.radToDeg(closestAngleRads);
    }

    public static int sign(float val) {
        return val > 0.0f ? 1 : (val < 0.0f ? -1 : 0);
    }

    public static int fastfloor(double val) {
        int xi = (int)val;
        return val < (double)xi ? xi - 1 : xi;
    }

    public static int fastfloor(float val) {
        int xi = (int)val;
        return val < (float)xi ? xi - 1 : xi;
    }

    public static int coorddivision(int value, int divisor) {
        return PZMath.fastfloor((float)value / (float)divisor);
    }

    public static int coordmodulo(int value, int divisor) {
        return value - PZMath.fastfloor((float)value / (float)divisor) * divisor;
    }

    public static float coordmodulof(float value, int divisor) {
        return value - (float)(PZMath.fastfloor(value / (float)divisor) * divisor);
    }

    public static float floor(float val) {
        return PZMath.fastfloor(val);
    }

    public static double floor(double val) {
        return PZMath.fastfloor(val);
    }

    public static float ceil(float val) {
        if (val >= 0.0f) {
            return (int)(val + 0.9999999f);
        }
        return (int)(val - 1.0E-7f);
    }

    public static float frac(float val) {
        float whole = PZMath.floor(val);
        return val - whole;
    }

    public static float wrap(float val, float range) {
        if (range == 0.0f) {
            return 0.0f;
        }
        if (range < 0.0f) {
            return 0.0f;
        }
        if (val < 0.0f) {
            float multipleNegative = -val / range;
            float fracUp = 1.0f - PZMath.frac(multipleNegative);
            return fracUp * range;
        }
        float multiple = val / range;
        float frac = PZMath.frac(multiple);
        return frac * range;
    }

    public static float wrap(float val, float min, float max) {
        float realMax = PZMath.max(max, min);
        float realMin = PZMath.min(max, min);
        float range = realMax - realMin;
        float relVal = val - realMin;
        float wrappedRelVal = PZMath.wrap(relVal, range);
        return realMin + wrappedRelVal;
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static float max(float a, float b, float c) {
        return PZMath.max(a, PZMath.max(b, c));
    }

    public static float max(float a, float b, float c, float d) {
        return PZMath.max(a, PZMath.max(b, PZMath.max(c, d)));
    }

    public static float max(float a, float b, float c, float d, float e) {
        return PZMath.max(a, PZMath.max(b, PZMath.max(c, PZMath.max(d, e))));
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int max(int a, int b, int c) {
        return PZMath.max(a, PZMath.max(b, c));
    }

    public static int max(int a, int b, int c, int d) {
        return PZMath.max(a, b, PZMath.max(c, d));
    }

    public static int max(int a, int b, int c, int d, int e) {
        return PZMath.max(a, b, c, PZMath.max(d, e));
    }

    public static float min(float a, float b) {
        return a > b ? b : a;
    }

    public static float min(float a, float b, float c) {
        return PZMath.min(a, PZMath.min(b, c));
    }

    public static float min(float a, float b, float c, float d) {
        return PZMath.min(a, PZMath.min(b, PZMath.min(c, d)));
    }

    public static float min(float a, float b, float c, float d, float e) {
        return PZMath.min(a, PZMath.min(b, PZMath.min(c, PZMath.min(d, e))));
    }

    public static int min(int a, int b) {
        return a > b ? b : a;
    }

    public static int min(int a, int b, int c) {
        return PZMath.max(a, PZMath.max(b, c));
    }

    public static int min(int a, int b, int c, int d) {
        return PZMath.max(a, b, PZMath.max(c, d));
    }

    public static int min(int a, int b, int c, int d, int e) {
        return PZMath.max(a, b, c, PZMath.max(d, e));
    }

    public static float abs(float val) {
        return val * (float)PZMath.sign(val);
    }

    public static int abs(int val) {
        return val * PZMath.sign(val);
    }

    public static boolean equal(float a, float b) {
        float delta = 1.0E-7f;
        return PZMath.equal(a, b, 1.0E-7f);
    }

    public static boolean equal(float a, float b, float delta) {
        float diff = b - a;
        float absDiff = PZMath.abs(diff);
        return absDiff < delta;
    }

    public static Matrix4f convertMatrix(org.joml.Matrix4f src, Matrix4f dst) {
        if (dst == null) {
            dst = new Matrix4f();
        }
        dst.m00 = src.m00();
        dst.m01 = src.m01();
        dst.m02 = src.m02();
        dst.m03 = src.m03();
        dst.m10 = src.m10();
        dst.m11 = src.m11();
        dst.m12 = src.m12();
        dst.m13 = src.m13();
        dst.m20 = src.m20();
        dst.m21 = src.m21();
        dst.m22 = src.m22();
        dst.m23 = src.m23();
        dst.m30 = src.m30();
        dst.m31 = src.m31();
        dst.m32 = src.m32();
        dst.m33 = src.m33();
        return dst;
    }

    public static org.joml.Matrix4f convertMatrix(Matrix4f src, org.joml.Matrix4f dst) {
        if (dst == null) {
            dst = new org.joml.Matrix4f();
        }
        return dst.set(src.m00, src.m01, src.m02, src.m03, src.m10, src.m11, src.m12, src.m13, src.m20, src.m21, src.m22, src.m23, src.m30, src.m31, src.m32, src.m33);
    }

    public static float step(float from, float to, float delta) {
        if (from > to) {
            return PZMath.max(from + delta, to);
        }
        if (from < to) {
            return PZMath.min(from + delta, to);
        }
        return from;
    }

    public static float angleBetween(Vector2 va, Vector2 vb) {
        float vax = va.x;
        float vay = va.y;
        float vbx = vb.x;
        float vby = vb.y;
        return PZMath.angleBetween(vax, vay, vbx, vby);
    }

    public static float angleBetween(float ax, float ay, float bx, float by) {
        float vaLength = PZMath.sqrt(ax * ax + ay * ay);
        if (vaLength == 0.0f) {
            return 0.0f;
        }
        float vbLength = PZMath.sqrt(bx * bx + by * by);
        if (vbLength == 0.0f) {
            return 0.0f;
        }
        return PZMath.angleBetweenNormalized(ax /= vaLength, bx /= vbLength, ay /= vaLength, by /= vbLength);
    }

    public static float angleBetweenNormalized(float ax, float bx, float ay, float by) {
        float dot = PZMath.clamp(ax * bx + ay * by, -1.0f, 1.0f);
        float absAngle = PZMath.acosf(dot);
        float cross = ax * by - ay * bx;
        int angleSign = PZMath.sign(cross);
        return absAngle * (float)angleSign;
    }

    public static float acosf(float a) {
        return (float)java.lang.Math.acos(a);
    }

    public static float calculateBearing(Vector3 fromPosition, Vector2 fromForward, Vector3 toPosition) {
        float toTargetX = toPosition.x - fromPosition.x;
        float toTargetY = toPosition.y - fromPosition.y;
        return PZMath.angleBetween(fromForward.x, fromForward.y, toTargetX, toTargetY) * 57.295776f;
    }

    public static Vector3f rotateVector(Vector3f vector, Quaternion quaternion, Vector3f result) {
        float qx = quaternion.x;
        float qy = quaternion.y;
        float qz = quaternion.z;
        float qw = quaternion.w;
        float vx = vector.x;
        float vy = vector.y;
        float vz = vector.z;
        return PZMath.rotateVector(vx, vy, vz, qx, qy, qz, qw, result);
    }

    public static Vector3f rotateVector(float vx, float vy, float vz, float qx, float qy, float qz, float qw, Vector3f result) {
        float dotUU = qx * qx + qy * qy + qz * qz;
        float dotUV = qx * vx + qy * vy + qz * vz;
        float crossUVx = qy * vz - qz * vy;
        float crossUVy = -(qx * vz - qz * vx);
        float crossUVz = qx * vy - qy * vx;
        result.x = 2.0f * dotUV * qx + (qw * qw - dotUU) * vx + 2.0f * qw * crossUVx;
        result.y = 2.0f * dotUV * qy + (qw * qw - dotUU) * vy + 2.0f * qw * crossUVy;
        result.z = 2.0f * dotUV * qz + (qw * qw - dotUU) * vz + 2.0f * qw * crossUVz;
        return result;
    }

    public static Vector2 rotateVector(float vx, float vy, float qx, float qy, float qz, float qw, Vector2 result) {
        float dotUU = qx * qx + qy * qy + qz * qz;
        float dotUV = qx * vx + qy * vy;
        float crossUVx = -(qz * vy);
        float crossUVy = qz * vx;
        result.x = 2.0f * dotUV * qx + (qw * qw - dotUU) * vx + 2.0f * qw * crossUVx;
        result.y = 2.0f * dotUV * qy + (qw * qw - dotUU) * vy + 2.0f * qw * crossUVy;
        return result;
    }

    public static Vector3 cross(Vector3 a, Vector3 b, Vector3 out) {
        return PZMath.cross(a.x, a.y, a.z, b.x, b.y, b.z, out);
    }

    private static Vector3 cross(float ux, float uy, float uz, float vx, float vy, float vz, Vector3 out) {
        float crossUVx = uy * vz - uz * vy;
        float crossUVy = -(ux * vz - uz * vx);
        float crossUVz = ux * vy - uy * vx;
        return out.set(crossUVx, crossUVy, crossUVz);
    }

    public static SideOfLine testSideOfLine(float x1, float y1, float x2, float y2, float px, float py) {
        float d = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
        return d > 0.0f ? SideOfLine.Left : (d < 0.0f ? SideOfLine.Right : SideOfLine.OnLine);
    }

    public static <E> void normalize(List<E> list, FloatGet<E> floatGet, FloatSet<E> floatSet) {
        int i;
        float[] values2 = new float[list.size()];
        for (i = 0; i < list.size(); ++i) {
            values2[i] = floatGet.get(list.get(i));
        }
        PZMath.normalize(values2);
        for (i = 0; i < list.size(); ++i) {
            floatSet.set(list.get(i), values2[i]);
        }
    }

    public static <E> void normalize(E[] list, FloatGet<E> floatGet, FloatSet<E> floatSet) {
        int i;
        float[] values2 = new float[list.length];
        for (i = 0; i < list.length; ++i) {
            values2[i] = floatGet.get(list[i]);
        }
        PZMath.normalize(values2);
        for (i = 0; i < list.length; ++i) {
            floatSet.set(list[i], values2[i]);
        }
    }

    public static float[] normalize(float[] weights) {
        int n = weights.length;
        float total = 0.0f;
        int i = n;
        while (--i >= 0) {
            total += weights[i];
        }
        if (total != 0.0f) {
            i = n;
            while (--i >= 0) {
                int n2 = i;
                weights[n2] = weights[n2] / total;
            }
        }
        return weights;
    }

    public static ArrayList<Double> normalize(ArrayList<Double> list) {
        int i;
        float[] values2 = new float[list.size()];
        for (i = 0; i < list.size(); ++i) {
            values2[i] = list.get(i).floatValue();
        }
        PZMath.normalize(values2);
        list.clear();
        for (i = 0; i < values2.length; ++i) {
            list.add(Double.valueOf(values2[i]));
        }
        return list;
    }

    public static float roundFloatPos(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; ++i) {
            pow *= 10;
        }
        float tmp = number * (float)pow;
        return (float)((int)(tmp - (float)((int)tmp) >= 0.5f ? tmp + 1.0f : tmp)) / (float)pow;
    }

    public static float roundFloat(float value, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; ++i) {
            pow *= 10;
        }
        float tmp = value * (float)pow;
        float tmpSub = tmp - (float)((int)tmp);
        return (float)((int)(value >= 0.0f ? (tmpSub >= 0.5f ? tmp + 1.0f : tmp) : (tmpSub >= -0.5f ? tmp : tmp - 1.0f))) / (float)pow;
    }

    public static int nextPowerOfTwo(int value) {
        if (value == 0) {
            return 1;
        }
        --value;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    public static float roundToNearest(float val) {
        int sign = PZMath.sign(val);
        return PZMath.floor(val + 0.5f * (float)sign);
    }

    public static int roundToInt(float val) {
        return (int)(PZMath.roundToNearest(val) + 1.0E-4f);
    }

    public static float roundToIntPlus05(float val) {
        return PZMath.floor(val) + 0.5f;
    }

    public static float roundFromEdges(float val) {
        float threshold = 0.2f;
        float valInt = PZMath.fastfloor(val);
        float valfrac = val - valInt;
        if (valfrac < 0.2f) {
            return valInt + 0.2f;
        }
        if (valfrac > 0.8f) {
            return valInt + 1.0f - 0.2f;
        }
        return val;
    }

    public static Vector3 closestVector3(float lx0, float ly0, float lz0, float lx1, float ly1, float lz1, float x, float y, float z) {
        Vector3 a = new Vector3(lx0, ly0, lz0);
        Vector3 b = new Vector3(lx1, ly1, lz1);
        Vector3 p = new Vector3(x, y, z);
        float abX = b.x - a.x;
        float abY = b.y - a.y;
        float abZ = b.z - a.z;
        float apX = p.x - a.x;
        float apY = p.y - a.y;
        float apZ = p.z - a.z;
        float dotProductAPAB = apX * abX + apY * abY + apZ * abZ;
        float dotProductABAB = abX * abX + abY * abY + abZ * abZ;
        float projectionScalar = dotProductAPAB / dotProductABAB;
        projectionScalar = Math.max(0.0f, Math.min(1.0f, projectionScalar));
        float closestX = a.x + projectionScalar * abX;
        float closestY = a.y + projectionScalar * abY;
        float closestZ = a.z + projectionScalar * abZ;
        return new Vector3(closestX, closestY, closestZ);
    }

    public static float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    public static boolean intersectLineSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2f intersection) {
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0f) {
            return false;
        }
        float yd = y1 - y3;
        float xd = x1 - x3;
        float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
        if (ua < 0.0f || ua > 1.0f) {
            return false;
        }
        float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
        if (ub < 0.0f || ub > 1.0f) {
            return false;
        }
        if (intersection != null) {
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    public static double closestPointOnLineSegment(float x1, float y1, float x2, float y2, float px, float py, double epsilon, Vector2f out) {
        double u = (double)((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (java.lang.Math.pow(x2 - x1, 2.0) + java.lang.Math.pow(y2 - y1, 2.0));
        if (Double.compare(u, epsilon) <= 0) {
            out.set(x1, y1);
            return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        }
        if (Double.compare(u, 1.0 - epsilon) >= 0) {
            out.set(x2, y2);
            return (px - x2) * (px - x2) + (py - y2) * (py - y2);
        }
        double xu = (double)x1 + u * (double)(x2 - x1);
        double yu = (double)y1 + u * (double)(y2 - y1);
        out.set((float)xu, (float)yu);
        return ((double)px - xu) * ((double)px - xu) + ((double)py - yu) * ((double)py - yu);
    }

    public static double closestPointsOnLineSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2f p1, Vector2f p2) {
        float t;
        float s;
        double eta = 1.0E-6;
        float rx = x3 - x1;
        float ry = y3 - y1;
        float ux = x2 - x1;
        float uy = y2 - y1;
        float vx = x4 - x3;
        float vy = y4 - y3;
        float ru = rx * ux + ry * uy;
        float rv = rx * vx + ry * vy;
        float uu = ux * ux + uy * uy;
        float vv = vx * vx + vy * vy;
        float uv = ux * vx + uy * vy;
        float det = uu * vv - uv * uv;
        if ((double)det < 1.0E-6 * (double)uu * (double)vv) {
            s = PZMath.clamp_01(ru / uu);
            t = 0.0f;
        } else {
            s = PZMath.clamp_01((ru * vv - rv * uv) / det);
            t = PZMath.clamp_01((ru * uv - rv * uu) / det);
        }
        float newS = PZMath.clamp_01((t * uv + ru) / uu);
        float newT = PZMath.clamp_01((s * uv - rv) / vv);
        p1.set(x1 + newS * ux, y1 + newS * uy);
        p2.set(x3 + newT * vx, y3 + newT * vy);
        return IsoUtils.DistanceToSquared(p1.x, p1.y, p2.x, p2.y);
    }

    public static enum SideOfLine {
        Left,
        OnLine,
        Right;

    }

    public static interface FloatGet<E> {
        public float get(E var1);
    }

    public static interface FloatSet<E> {
        public void set(E var1, float var2);
    }
}

