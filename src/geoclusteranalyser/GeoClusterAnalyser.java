package geoclusteranalyser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sajiban_18
 */
public class GeoClusterAnalyser
{
    public static void main(String[] args) throws IOException
    {
        int Width, Height;
        String CSVPath;
        
        if(args.length == 3)
        {
            Width = Integer.parseInt(args[0]);
            Height = Integer.parseInt(args[1]);
            CSVPath = args[2];
        }
        else
        {
            System.out.println("Require GeoBlock width, height and CSV file path!");
            return;
        }
        
        GeoBlock GB = new GeoBlock(Width, Height);
        boolean Load = GB.LoadCSVData(CSVPath);
        if(Load == false) return;
       
        GeoCluster LargestCluster = GB.getLargestCluster();
        System.out.println("The Geos in the largest cluster of occupied Geos for this GeoBlock are: ");
        System.out.print(LargestCluster.Output());
    }
}

class Geo
{
    public int ID;
    public String Name;
    public Date OccupiedDate;
    GeoBlock GBlock;
    Geo Left, Right, Down, Up;
        
    public Geo(int id, GeoBlock gb)
    {
        ID = id;
        GBlock = gb;
        Left = null; Right = null; Down = null; Up = null; 
    }
    
    public void OccupyGeo(String name, Date odate)
    {
        Name = name;
        OccupiedDate = odate;
    }
    
    public Geo getAdjGeo(String position)
    {
        if("Left".equals(position)) return Left;
        if("Right".equals(position)) return Right;
        if("Down".equals(position)) return Down;
        if("Up".equals(position)) return Up;    
        return null;
    }
    
    public void setAdjGeo(String position, Geo geo)
    {
        if("Left".equals(position)) Left = geo;
        if("Right".equals(position)) Right = geo;
        if("Down".equals(position)) Down = geo;
        if("Up".equals(position)) Up = geo;
    }
    
    public GeoCluster getCluster(GeoCluster cluster)
    {
        if((Name != null) && (GBlock.CheckCluster(this) == false))
        {
            cluster.AddGeo(this);
            if(Left != null) Left.getCluster(cluster);
            if(Right != null) Right.getCluster(cluster);
            if(Down != null) Down.getCluster(cluster);
            if(Up != null) Up.getCluster(cluster);
            return cluster;
        }
        return null;
    }
    
    public String getName(){return Name;}
    public void setName(String name){this.Name = name;}
    public Date getDate(){return OccupiedDate;}
    public void setDate(Date odate){this.OccupiedDate = odate;}
    
    public String output()
    {
        DateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
        String ODate = DF.format(OccupiedDate);
        return(Integer.toString(ID) + ", " + Name + ", " + ODate);
    }
}


class GeoBlock
{
    public int Width, Height;
    ArrayList<Geo> Geos;
    ArrayList<GeoCluster> GeoClusters;
    GeoCluster Cluster, LargestCluster;
    
    public GeoBlock(int width, int height)
    {
        Width = width;
        Height = height;
        int Size = width*height;
        GeoClusters = new ArrayList<>();
        Geos = new ArrayList<>();
        
        for(int i=0; i<Size; i++){Geos.add(new Geo(i,this));}
        for(int h=0; h<Height; h++)
        {
            for(int w=0; w<Width; w++)
            {
                int id = (Width * h) + w;
                if(w > 0) Geos.get(id).setAdjGeo("Left", Geos.get(id - 1));
                if(w < (Width - 1)) Geos.get(id).setAdjGeo("Right", Geos.get(id + 1));
                if(h < (Height - 1)) Geos.get(id).setAdjGeo("Up", Geos.get(id + Width));
                if(h > 0) Geos.get(id).setAdjGeo("Down", Geos.get(id - Width));
            }
        }      
    }
    
    public boolean LoadCSVData(String csvPath)
    {
        String Line;
        String[] Data;
        BufferedReader BR;
        try
        {
            BR = new BufferedReader(new FileReader(csvPath));
            while ((Line = BR.readLine()) != null)
            {
                Data = Line.split(",");
                int ID = Integer.parseInt(Data[0]);
                String Name = Data[1];
                Date ODate = null;
                try
                {
                    DateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
                    ODate = DF.parse(Data[2]);
                }
                catch (ParseException ex)
                {
                    System.out.println("Date can not be parsed: " + Data[2]);
                    Logger.getLogger(GeoBlock.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.OccupyBlock(ID, Name, ODate);
            }
            BR.close();
        }
        catch (FileNotFoundException ex)
        {
            System.out.println("File not found: " + csvPath);
            Logger.getLogger(GeoBlock.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        catch (IOException ex)
        {
            System.out.println("Opening/ closing file error: " + csvPath);
            Logger.getLogger(GeoBlock.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        this.CalculateClusters();
        return true;
    }
    
    public void OccupyBlock(int id, String name, Date odate)
    {
        Geos.get(id).OccupyGeo(name, odate);
    }
    
    boolean CheckCluster(Geo geo)
    {
        for(int i=0; i<GeoClusters.size(); i++)
        {
            if(GeoClusters.get(i).GotGeo(geo)) return true;
        }
        return false;
    }
    
    public GeoCluster getCluster(int id)
    {
        Geo geo = Geos.get(id);
        if(CheckCluster(geo) == false && geo.Name != null)
        {
            GeoCluster gc = new GeoCluster();
            GeoClusters.add(gc);
            geo.getCluster(gc);
            return gc;
        }
        return null;
    }
    
    public void CalculateClusters()
    {
        int Size = Width * Height;
        for(int i=0; i<Size; i++) getCluster(i);
    }
    
    public GeoCluster getLargestCluster()
    {
        GeoCluster LargestGC = null;
        for(int i=0; i<GeoClusters.size(); i++)
        {
            if(LargestGC == null) LargestGC = GeoClusters.get(i);
            else if(LargestGC.getGeoCluster().size() < GeoClusters.get(i).getGeoCluster().size()) LargestGC = GeoClusters.get(i);
        }
        LargestCluster = LargestGC;
        return LargestGC;
    }
}


class GeoCluster
{
    public ArrayList<Geo> GeoCluster;
    public GeoCluster() {GeoCluster = new ArrayList<>();}
    public ArrayList<Geo> getGeoCluster() {return GeoCluster;}
    public void AddGeo(Geo geo) {GeoCluster.add(geo);}
    public void RemoveGeo(Geo geo) {GeoCluster.remove(geo);}
    
    public boolean GotGeo(Geo geo)
    {
        for(int i=0; i<GeoCluster.size(); i++)
        {
            if(GeoCluster.get(i) == geo) return true;
        }
        return false;
    }
    
    public String Output()
    {
        String OString = "";
        for(int i=0; i<GeoCluster.size(); i++) {OString += GeoCluster.get(i).output() + "\n";}
        return OString;
    }
}