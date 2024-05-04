package project.coca.domain.tag;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@ToString
@NoArgsConstructor
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TAG_ID")
    private Long id;
    @Column(name = "FIELD", length = 10, nullable = false)
    private String field;
    @Column(name = "NAME", length = 10, nullable = false)
    private String name;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL)
    private List<GroupTag> groupTags = new ArrayList<>();

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL)
    private List<Interest> interest = new ArrayList<>();
}
