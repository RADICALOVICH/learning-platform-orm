package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "profiles")
public class Profile extends AbstractEntity<Long> {
    @Id
    private Long id;

    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_profiles_user"))
    private User user;

    @Setter
    @Column(columnDefinition = "text")
    private String bio;

    @Setter
    @Column(length = 500)
    private String avatarUrl;

    @Setter
    @Column(length = 30)
    private String phone;

    @Setter
    @Column(length = 100)
    private String city;

    @Setter
    private LocalDate birthDate;

    public Profile(User user) {
        this.user = user;
    }
}
