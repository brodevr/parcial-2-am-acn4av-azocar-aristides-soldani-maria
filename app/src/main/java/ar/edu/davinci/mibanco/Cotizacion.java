package ar.edu.davinci.mibanco;

import com.google.gson.annotations.SerializedName;

/**
 * Cotización de un tipo de dólar, tal como la devuelve DolarApi.
 * Gson completa los campos a partir del JSON usando los nombres de @SerializedName.
 */
public class Cotizacion {

    @SerializedName("casa")
    private String casa;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("compra")
    private Double compra;

    @SerializedName("venta")
    private Double venta;

    @SerializedName("fechaActualizacion")
    private String fechaActualizacion;

    public String getCasa() { return casa; }
    public String getNombre() { return nombre; }
    public Double getCompra() { return compra; }
    public Double getVenta() { return venta; }
    public String getFechaActualizacion() { return fechaActualizacion; }
}