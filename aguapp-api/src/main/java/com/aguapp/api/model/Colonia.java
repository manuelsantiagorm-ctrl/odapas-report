package com.aguapp.api.model;

/**
 * Colonia donde se pueden levantar reportes de agua.
 * Se usa un "record" de Java: una clase inmutable que ya trae
 * constructor, getters, equals, hashCode y toString.
 */
public record Colonia(String id, String nombre, String estadoServicio) {
}
