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
    // creada a mano desde el árbol del admin), y puede haber más de un nivel de anidamiento en
    // el medio (ej. "Singles"/"Sellados" cuelgan de "Magic the gathering", no de la raíz
    // directamente). Sube por parent hasta encontrar rootShortName o llegar al tope.
    //
    // La categoría raíz real está auto-referenciada en la base (su propio id_parent apunta a
    // sí misma, en vez de ser NULL) -- sin el corte de "parent == this" de abajo, cualquier
    // producto fuera de la rama SIN/PSL (ej. accesorios) colgaría el request en un loop
    // infinito la primera vez que se llame a este método con un rootShortName que no matchea.
    public boolean isDescendantOfOrSelf(String rootShortName) {
        Category c = this;
        while (c != null) {
            if (rootShortName.equals(c.getShortName())) return true;
            Category parent = c.getParent();
            if (parent == null || parent.getId().equals(c.getId())) return false;
            c = parent;
        }
        return false;
    }
}
