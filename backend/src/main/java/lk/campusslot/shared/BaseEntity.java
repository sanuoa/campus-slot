package lk.campusslot.shared;

import jakarta.persistence.*;

/** Inheritance shares database identity without forcing unrelated domain types together. */
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
}
