package com.simple.ipeer.config;

import com.simple.ipeer.config.Property.Type;
import java.util.Arrays;

/**
 *
 * @author iPeer
 */
public class Property {
    
    public enum Type {
	STRING,
	INTEGER,
	FLOAT,
	DOUBLE,
	BOOLEAN;
	
	public static Type parseType(char c) {
	    for (int i = 0; i < values().length; i++)
		if (values()[i].getID() == c)
		    return values()[i];
	    return STRING;
	}
	
	public char getID() { return name().charAt(0); }
    }
    
    private Type type;
    private String name;
    private String value;
    private String defaultValue;
    private final boolean readFromFile;
    private final boolean isList;
    private String[] values;
    private String[] validValues;
    private String[] defaultValues;
    
    /* Single entries */
    
    public Property (String pName, String pValue, Type type) throws UnsupportedConfigOperationException {
	this(pName, pValue, type, false, new String[0]);
    }
    
    public Property (String pName, String pValue, Type type, boolean read) throws UnsupportedConfigOperationException {
	this(pName, pValue, type, read, new String[0]);
    }
    
    public Property (String pName, String pValue, Type type, String[] validValues) throws UnsupportedConfigOperationException {
	this(pName, pValue, type, false, validValues);
    }
    
    public Property (String pName, String pValue, Type type, boolean read, String[] validValues) throws UnsupportedConfigOperationException {
	setName(pName);
	this.value = pValue;
	this.type = type;
	readFromFile = read;
	isList = false;
	this.validValues = validValues;
	this.defaultValue = value;
	this.defaultValues = new String[0];
    }
    
    /* List entries */
    
    public Property (String n, String[] v, Type t) throws UnsupportedConfigOperationException {
	this(n, v, t, false, new String[0]);
    }
    
    public Property (String n, String[] v, Type t, boolean r) throws UnsupportedConfigOperationException {
	this(n, v, t, r, new String[0]);
    }
    
    public Property (String n, String[] v, Type t, String[] vV) throws UnsupportedConfigOperationException {
	this(n, v, t, false, vV);
    }
    
    public Property (String n, String[] v, Type t, boolean r, String[] vV) throws UnsupportedConfigOperationException {
	setName(n);
	this.value = "";
	this.values = Arrays.copyOf(v, v.length);
	this.type = t;
	readFromFile = r;
	isList = true;
	this.defaultValue = "";
	for (String s : v)
	    this.defaultValue += (!this.defaultValue.equals("") ? ", " : "")+s;
	this.defaultValue = "{"+this.defaultValue+"}";
	this.defaultValues = Arrays.copyOf(v, v.length);
	this.validValues = vV;
    }
    
    /*public void setName(String name) {
	this.name = name;
    }*/
    
    public Property setDefaultValue(String v) {
	this.defaultValue = v;
	return this;
    }
    
    public Property setDefaultValues(String[] v) {
	this.defaultValue = "";
	for (String s : v)
	    this.defaultValue += (!this.defaultValue.equals("") ? ", " : "")+s;
	this.defaultValue = "{"+this.defaultValue+"}";
	this.defaultValues = Arrays.copyOf(v, v.length);
	return this;
    }
    
    public Property setDefault(String v) {
	setDefaultValue(v);
	return this;
    }
    
    public Property setDefaults(String[] v) {
	setDefaultValues(v);
	return this;
    }
    
    public Property setDefaultValue (int v) {
	setDefaultValue(Integer.toString(v));
	return this;
    }
    
    public Property setDefaultValues(int[] v) {
	String[] _tmp = new String[v.length];
	for (int i = 0; i < v.length; i++)
	    _tmp[i] = Integer.toString(v[i]);
	setDefaultValues(_tmp);
	return this;
    }
    
    public Property setDefaultValue (double v) {
	setDefaultValue(Double.toString(v));
	return this;
    }
    
    public Property setDefaultValues(double[] v) {
	String[] _tmp = new String[v.length];
	for (int i = 0; i < v.length; i++)
	    _tmp[i] = Double.toString(v[i]);
	setDefaultValues(_tmp);
	return this;
    }
    
    public Property setDefaultValue (float v) {
	setDefaultValue(Float.toString(v));
	return this;
    }
    
    public Property setDefaultValues(float[] v) {
	String[] _tmp = new String[v.length];
	for (int i = 0; i < v.length; i++)
	    _tmp[i] = Float.toString(v[i]);
	setDefaultValues(_tmp);
	return this;
    }
    
    public Property setDefaultValue (boolean v) {
	setDefaultValue(Boolean.toString(v));
	return this;
    }
    
    public Property setDefaultValues(boolean[] v) {
	String[] _tmp = new String[v.length];
	for (int i = 0; i < v.length; i++)
	    _tmp[i] = Boolean.toString(v[i]);
	setDefaultValues(_tmp);
	return this;
    }
    
    public Property setToDefault() {
	this.value = this.defaultValue;
	this.values = Arrays.copyOf(this.defaultValues, this.defaultValues.length);
	return this;
    }
    
    public Property setValidValues(String[] vV) {
	this.validValues = vV;
	return this;
    }
    
    public String[] getValideValues() { return this.validValues; }
    
    public String getDefault() throws UnsupportedConfigOperationException {
	if (this.isList())
	    throw new UnsupportedConfigOperationException("Cannot get singular default on a List entry. Use getDefaults() instead");
	return this.defaultValue;
    }
    
    public String[] getDefaults() throws UnsupportedConfigOperationException {
	if (!this.isList())
	    throw new UnsupportedConfigOperationException("Cannot get defaults list on an entry that only accepts one value. Use getDefault() instead");
	return this.defaultValues;
    }
    
    public boolean isIntValue() {
	try {
	    Integer.parseInt(this.value);
	    return true;
	}
	catch (NumberFormatException _e) { return false; }
    }
    
    public boolean isBooleanValue() {
	String[] bools = {"true", "false"};
	return Arrays.asList(bools).contains(value.toLowerCase());
    }
    
    public boolean isDoubleValue() {
	try {
	    Double.parseDouble(this.value);
	    return true;
	}
	catch (NumberFormatException _e) { return false; }
    }
    
    public boolean isFloatValue() {
	try {
	    Float.parseFloat(this.value);
	    return true;
	}
	catch (NumberFormatException _e) { return false; }
    }
       
    public boolean isIntList() {
	if (isList && this.type == Type.INTEGER) {
	    for (String v : values) {
		try {
		    Integer.parseInt(v);
		}
		catch (NumberFormatException _e) { return false; }
	    }
	}
	return isList && this.type == Type.INTEGER;
    }
    
    public boolean isDoubleList() {
	return false;
	// TODO
    }
    
    public boolean isFloatList() {
	return false;
	// TODO
    }
    
    public boolean isBooleanList() {
	return false;
	// TODO
    }
    
    public String[] getStringList() { return this.values; }
    
    public int[] getIntList() {
	return new int[0];
	// TODO
    }
    
    public float[] getFloatList() {
	return new float[0];
	// TODO
    }
    public double[] getDoubleList() {
	return new double[0];
	// TODO
    }
    public boolean[] getBooleanList() {
	return new boolean[0];
	// TODO
    }
    
    public int getInt() throws UnsupportedConfigOperationException {
	if (!this.isIntValue())
	    throw new UnsupportedConfigOperationException("Attempted to return property that is not an Integer as an Integer");
	return Integer.valueOf(this.value);
    }
    
    public int getInt(int _d) {
	try {
	    return getInt();
	}
	catch (UnsupportedConfigOperationException _e) { return _d; }
	
    }
    
    public float getFloat() throws UnsupportedConfigOperationException {
	if (!this.isFloatValue())
	    throw new UnsupportedConfigOperationException("Attempted to return property that is not a Float as a Float");
	return Float.valueOf(this.value);
    }
    
    public float getFloat(float _d) {
	try {
	    return getFloat();
	}
	catch (UnsupportedConfigOperationException _e) { return _d; }
	
    }
    
    public double getDouble() throws UnsupportedConfigOperationException {
	if (!this.isDoubleValue())
	    throw new UnsupportedConfigOperationException("Attempted to return property that is not a Double as a Double");
	return Double.valueOf(this.value);
    }
    
    public double getDouble(double _d) {
	try {
	    return getDouble();
	}
	catch (UnsupportedConfigOperationException _e) { return _d; }
	
    }
    
    public boolean getBoolean() throws UnsupportedConfigOperationException {
	if (!this.isBooleanValue())
	    throw new UnsupportedConfigOperationException("Attempted to return property that is not a Boolean as a Boolean");
	return Boolean.valueOf(this.value);
    }
    
    public boolean getBoolean(boolean _d) {
	try {
	    return getBoolean();
	}
	catch (UnsupportedConfigOperationException _e) { return _d; }
	
    }
    
    public int getIntValue() throws UnsupportedConfigOperationException { return getInt(); }
    public float getFloatValue() throws UnsupportedConfigOperationException { return getFloat(); }
    public double getDoubleValue() throws UnsupportedConfigOperationException { return getDouble(); }
    public boolean getBooleanValue() throws UnsupportedConfigOperationException { return getBoolean(); }
    public int getIntValue(int _d) throws UnsupportedConfigOperationException { return getInt(_d); }
    public float getFloatValue(float _d) throws UnsupportedConfigOperationException { return getFloat(_d); }
    public double getDoubleValue(double _d) throws UnsupportedConfigOperationException { return getDouble(_d); }
    public boolean getBooleanValue(boolean _d) throws UnsupportedConfigOperationException { return getBoolean(_d); }
    public String getString() { return this.value; }
    public String getStringValue() { return getString(); }
    public String getValue() { return getString(); }
    public String getCurrentValue() { return getString(); }
    @Override
    public String toString() { return getString(); }
    
    public boolean wasRead() { return readFromFile; }
    public boolean wasReadFromFile() { return wasRead(); }
    public boolean read() { return wasRead(); }
    
    public Type getType() { return this.type; }
    public Type type() { return getType(); }
    
    public boolean isList() { return isList; }
    public boolean list() { return isList(); }
    
    public String getName() { return this.name; }
    public void setName(String n) throws UnsupportedConfigOperationException { 
	if (n.equals("") || n.length() < 1) 
	    throw new UnsupportedConfigOperationException("Property name cannot be an empty, nulled, or zero-length string");
    }
	
    
}
