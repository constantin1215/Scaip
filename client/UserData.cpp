#include "UserData.h"

UserData* UserData::userData_ = nullptr;

void UserData::setJWT(QString JWT)
{
    this->JWT = JWT;
}

void UserData::setUsername(QString username)
{
    this->username = username;
}

void UserData::setFirstName(QString firstName)
{
    this->firstName = firstName;
}

void UserData::setLastName(QString lastName)
{
    this->lastName = lastName;
}

void UserData::setEmail(QString email)
{
    this->email = email;
}

void UserData::setJSON(QJsonObject json)
{
    this->rawJson = json;
    this->groups = json["groups"].toArray();
}

QString UserData::getJWT()
{
    return this->JWT;
}

QString UserData::getUsername()
{
    return this->username;
}

QString UserData::getFirstName()
{
    return this->firstName;
}

QString UserData::getLastName()
{
    return this->lastName;
}

QString UserData::getEmail()
{
    return this->email;
}

QJsonArray UserData::getGroups()
{
    return this->groups;
}

QJsonObject UserData::getJSON()
{
    return this->rawJson;
}

void UserData::printData()
{
    qDebug() <<
        "\nUsername: " << this->username <<
        "\nEmail: " << this->email <<
        "\nFirstName: " << this->firstName <<
        "\nLastName: " << this->lastName;
}

UserData *UserData::getInstance()
{
    if(userData_ == nullptr) {
        userData_ = new UserData();
    }

    return userData_;
}
