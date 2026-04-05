package com.w2w.api.positiongroup.model;

import com.w2w.api.position.model.Position;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_group")
@SequenceGenerator(name = "skill_group_id_seq", sequenceName = "skill_group_id_seq", allocationSize = 1)
public class PositionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skill_group_id_seq")
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "description")
    private String description;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToMany
    @JoinTable(
        name = "group_skill",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Position> positions = new ArrayList<>();

    public PositionGroup() {}

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}
