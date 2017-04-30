/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import lu.fisch.unimozer.Diagram;

/**
 *
 * @author robert.fisch
 */
public class Test {
    
    public static void main(String[] args)
    {
        Diagram d = new Diagram();
        System.out.println("Created diagram");
        System.out.println("Opening: ");
        d.open("C:\\Users\\robert.fisch\\Desktop\\Demo");
        System.out.println("Opened Project");
        d.runFast();
        
    }
}
