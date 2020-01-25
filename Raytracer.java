import java.io.*;
import java.util.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;

public class Raytracer {

    private static Vector3D eye, look, up;
    private static HashMap<String, Integer>
            bounds = new HashMap<>(),
            res = new HashMap<>();
    private static HashMap<String, Double> ambient = new HashMap<>();
    private static ArrayList<HashMap<String, Double>>
            lights = new ArrayList<>(),
            spheres = new ArrayList<>();
    private static int d;

    public static void main(String[] args) {
        try {

            File driverFile = new File(args[0]);
            Scanner scan = new Scanner(driverFile);
            while (scan.hasNextLine()) {
                scanNextLine(scan.nextLine().trim());
            }

            int[][] img = new int[res.get("height")][res.get("width") * 3];
            for (int i = 0; i < res.get("height"); ++i) {
                for (int j = 0; j < res.get("width"); ++j) {
                    Vector3D[] pixelRay = pixelRay(i, j);

                    int sphIndex = -1;
                    double sphTValue = -1.0;
                    Vector3D sphPt = null;
                    for (int k = 0; k < spheres.size(); ++k) {
                        HashMap<String, Double> sph = spheres.get(k);

                        Object[] test = raySphereTest(pixelRay, sph);

                        if ((boolean)test[0] == true) {
                            double t = (double)test[1];
                            if (sphIndex == -1 || sphTValue > t) {
                                sphIndex = k;
                                sphTValue = t;
                                sphPt = (Vector3D)test[2];
                            }
                        }
                    }

                    if (sphIndex != -1) {
                        Object[] res = raySphereRGB(pixelRay, spheres.get(sphIndex),
                                sphPt);
                        Vector3D vec = (Vector3D)res[1];
                        img[i][j*3] = (int)(vec.getX() * 255.0);
                        img[i][(j*3)+1] = (int)(vec.getY() * 255.0);
                        img[i][(j*3)+2] = (int)(vec.getZ() * 255.0);
                    }

                }
            }

            writeImg(img, args[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scanNextLine(String line) {
        if (line.isEmpty() || line.charAt(0) == '#')
            return;
        String[] data = line.split(" ");
        switch (data[0]) {
            case "eye":
                eye = new Vector3D(Integer.parseInt(data[1]),
                        Integer.parseInt(data[2]), Integer.parseInt(data[3]));
                break;
            case "look":
                look = new Vector3D(Integer.parseInt(data[1]),
                        Integer.parseInt(data[2]), Integer.parseInt(data[3]));
                break;
            case "up":
                up = new Vector3D(Integer.parseInt(data[1]),
                        Integer.parseInt(data[2]), Integer.parseInt(data[3]));
                break;
            case "d":
                d = Integer.parseInt(data[1]);
                break;
            case "bounds":
                bounds.put("left", Integer.parseInt(data[1]));
                bounds.put("right", Integer.parseInt(data[2]));
                bounds.put("bottom", Integer.parseInt(data[3]));
                bounds.put("top", Integer.parseInt(data[4]));
                break;
            case "res":
                res.put("width", Integer.parseInt(data[1]));
                res.put("height", Integer.parseInt(data[2]));
                break;
            case "ambient":
                ambient.put("red", Double.parseDouble(data[1]));
                ambient.put("green", Double.parseDouble(data[2]));
                ambient.put("blue", Double.parseDouble(data[3]));
                break;
            case "light":
                HashMap<String, Double> l = new HashMap<>();
                l.put("x", Double.parseDouble(data[1]));
                l.put("y", Double.parseDouble(data[2]));
                l.put("z", Double.parseDouble(data[3]));
                l.put("w", Double.parseDouble(data[4]));
                l.put("red", Double.parseDouble(data[5]));
                l.put("green", Double.parseDouble(data[6]));
                l.put("blue", Double.parseDouble(data[7]));
                lights.add(l);
                break;
            case "sphere":
                HashMap<String, Double> s = new HashMap<>();
                s.put("x", Double.parseDouble(data[1]));
                s.put("y", Double.parseDouble(data[2]));
                s.put("z", Double.parseDouble(data[3]));
                s.put("r", Double.parseDouble(data[4]));
                s.put("Ka_r", Double.parseDouble(data[5]));
                s.put("Ka_g", Double.parseDouble(data[6]));
                s.put("Ka_b", Double.parseDouble(data[7]));
                s.put("Kd_r", Double.parseDouble(data[8]));
                s.put("Kd_g", Double.parseDouble(data[9]));
                s.put("Kd_b", Double.parseDouble(data[10]));
                s.put("Ks_r", Double.parseDouble(data[11]));
                s.put("Ks_g", Double.parseDouble(data[12]));
                s.put("Ks_b", Double.parseDouble(data[13]));
                s.put("Kr_r", Double.parseDouble(data[14]));
                s.put("Kr_g", Double.parseDouble(data[15]));
                s.put("Kr_b", Double.parseDouble(data[16]));
                spheres.add(s);
                break;
        }
    }

    private static Object[] raySphereTest(Vector3D[] ray, HashMap<String, Double> sph) {
        double r = sph.get("r");
        Vector3D c = new Vector3D(sph.get("x"), sph.get("y"), sph.get("z"));
        Vector3D Tv = c.subtract(ray[0]);
        double v = Tv.dotProduct(ray[1]);
        double csq = Tv.dotProduct(Tv);
        double disc = (r*r) - (csq - (v*v));
        if (disc < 0)
            return new Object[] { false };
        else {
            double t = v - Math.sqrt(disc);
            Vector3D pt = ray[0].add(ray[1].scalarMultiply(t));
            return new Object[] { true, t, pt };
        }
    }

    private static Object[] raySphereRGB(Vector3D[] ray, HashMap<String, Double> sph,
            Vector3D ptos)
    {
        //Object[] hitp = raySphereTest(ray, sph);
        //if ((boolean)hitp[0] == true) {
            //Vector3D ptos = (Vector3D)hitp[2];
            Vector3D c = new Vector3D(sph.get("x"), sph.get("y"), sph.get("z"));
            Vector3D snrm = ptos.subtract(c);
            snrm = snrm.normalize();
            Vector3D amb = new Vector3D(ambient.get("red"), ambient.get("green"),
                    ambient.get("blue"));
            Vector3D ka = new Vector3D(sph.get("Ka_r"), sph.get("Ka_g"),
                    sph.get("Ka_b"));
            Vector3D kd = new Vector3D(sph.get("Kd_r"), sph.get("Kd_g"),
                    sph.get("Kd_b"));
            Vector3D color = pairwiseProduct(amb, ka);
            for (HashMap<String, Double> lt : lights) {
                Vector3D ptL = new Vector3D(lt.get("x"), lt.get("y"), lt.get("z"));
                Vector3D emL = new Vector3D(lt.get("red"), lt.get("green"),
                        lt.get("blue"));
                Vector3D toL = ptL.subtract(ptos);
                toL = toL.normalize();
                if (snrm.dotProduct(toL) > 0.0) {
                    color = color.add(pairwiseProduct(kd, emL).scalarMultiply(
                            snrm.dotProduct(toL)
                    ));
                }
            }

            return new Object[] { true, color };
        //} else {
        //    return new Object[] { false };
        //}
    }

    private static Vector3D[] pixelRay(int i, int j) {

        double px = ((double)i / ((double)res.get("width") - 1)) *
                (bounds.get("right") - bounds.get("left")) + bounds.get("left");
        double py = ((double)j / ((double)res.get("height") - 1)) *
                (bounds.get("bottom") - bounds.get("top")) + bounds.get("top");

        Vector3D CWv = eye.subtract(look);
        CWv = CWv.normalize();
        Vector3D CUv = up.crossProduct(CWv);
        CUv = CUv.normalize();
        Vector3D CVv = CWv.crossProduct(CUv);
        Vector3D Lv = eye.add( CWv.scalarMultiply(d) ).add( CUv.scalarMultiply(px) )
                .add( CVv.scalarMultiply(py) );
        Vector3D Uv = Lv.subtract(eye);
        Uv = Uv.normalize();
        return new Vector3D[] { Lv, Uv };

    }

    private static void writeImg(int[][] img, String filepath) {
        try {

            FileWriter writer = new FileWriter(filepath, false);
            writer.write("P3\n");
            writer.write((img[0].length / 3) +" "+ img.length +" 255\n");
            for (int w = 0; w < img[0].length; w += 3) {
                for (int l = 0; l < img.length; ++l) {
                    writer.write(img[l][w] +" "+ img[l][w + 1] +" "+
                            img[l][w + 2] +" ");
                }
            }
            /*for (int i = 0; i < img.length; ++i) {
                for (int j = 0; j < img[0].length; ++j) {
                    writer.write(img[i][j] + " ");
                }
                writer.write("\n");
            }*/
            writer.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static Vector3D pairwiseProduct(Vector3D u, Vector3D v) {
        return new Vector3D(u.getX() * v.getX(), u.getY() * v.getY(),
                u.getZ() * v.getZ());
    }

}
