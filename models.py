from sqlalchemy import Column, Integer, String, ForeignKey, Text, DateTime, func, Enum
from sqlalchemy.ext.hybrid import hybrid_property
from sqlalchemy.orm import relationship
from database import Base
from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, nullable=False)
    email = Column(String, unique=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(DateTime, default=func.now())

    # связь с сыгранными играми
    played_games = relationship("PlayedGame", back_populates="user")

    @hybrid_property
    def games_played_count(self):
        # возвращает число записей в played_games
        return len(self.played_games)

    def set_password(self, password: str):
        self.password_hash = pwd_context.hash(password)

    def verify_password(self, password: str) -> bool:
        return pwd_context.verify(password, self.password_hash)


class Genre(Base):
    __tablename__ = "genres"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, nullable=False)


class Game(Base):
    __tablename__ = "games"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, unique=True, nullable=False)
    min_players = Column(Integer, nullable=False)
    max_players = Column(Integer, nullable=False)
    play_time = Column(Integer, nullable=True)
    genre_id = Column(Integer, ForeignKey("genres.id"), nullable=True)
    description = Column(Text, nullable=True)
    image_url = Column(String, nullable=True)
    created_at = Column(DateTime, default=func.now())

    genre = relationship("Genre", back_populates="games")
    played_games = relationship("PlayedGame", back_populates="game")


# Добавляем обратную связь в модель Genre
Genre.games = relationship("Game", back_populates="genre")


class UserGame(Base):
    __tablename__ = "user_games"
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    game_id = Column(Integer, ForeignKey("games.id"), primary_key=True)


class Rating(Base):
    __tablename__ = "ratings"
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    game_id = Column(Integer, ForeignKey("games.id"), primary_key=True)
    rating = Column(Integer, nullable=False)
    review = Column(Text, nullable=True)
    rated_at = Column(DateTime, default=func.now())


class FriendRequest(Base):
    __tablename__ = "friends"
    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    receiver_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    status = Column(Enum("pending", "accepted", "rejected", name="friend_status"), default="pending")

    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])


class Group(Base):
    __tablename__ = "groups"
    id = Column(Integer, primary_key=True, index=True)
    creator_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime, default=func.now())

    creator = relationship("User")


class GroupMember(Base):
    __tablename__ = "group_members"
    group_id = Column(Integer, ForeignKey("groups.id"), primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)


class GroupInvitation(Base):
    __tablename__ = "group_invitations"
    id = Column(Integer, primary_key=True, index=True)
    group_id = Column(Integer, ForeignKey("groups.id"), nullable=False)
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    receiver_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    status = Column(Enum("pending", "accepted", "rejected", name="invitation_status"), default="pending")

    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])
    group = relationship("Group")


class PlayedGame(Base):
    __tablename__ = "played_games"
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    game_id = Column(Integer, ForeignKey("games.id"), primary_key=True)
    last_played = Column(DateTime, default=func.now())

    user = relationship("User", back_populates="played_games")
    game = relationship("Game", back_populates="played_games")
