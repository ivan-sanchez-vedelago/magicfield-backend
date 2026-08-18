package com.magicfield.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parent")
    private Category parent;

    @Column(nullable = false, unique = true)
    private String shortName;

    public Category() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    // shortName es único por categoría (ver @Column arriba) -- un producto sellado nunca tiene
    // category.shortName == "PSL" directamente, sino el de su subcategoría hoja (ej. "PRECON",
    // creada a mano desde el árbol del admin). Sube por parent hasta encontrar rootShortName o
    // llegar a la raíz, para poder preguntar "¿esta hoja cuelga de PSL/SIN?" sin asumir que el
    // producto está categorizado de forma plana.
    public boolean isDescendantOfOrSelf(String rootShortName) {
        Category c = this;
        while (c != null) {
            if (rootShortName.equals(c.getShortName())) return true;
            c = c.getParent();
        }
        return false;
    }
}
