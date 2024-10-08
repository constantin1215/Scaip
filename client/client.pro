QT       += core gui websockets multimedia multimediawidgets

greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

CONFIG += c++17

# You can make your code fail to compile if it uses deprecated APIs.
# In order to do so, uncomment the following line.
#DEFINES += QT_DISABLE_DEPRECATED_BEFORE=0x060000    # disables all the APIs deprecated before Qt 6.0.0

SOURCES += \
    AudioWSClient.cpp \
    CallWidget.cpp \
    EventHandler.cpp \
    MembersListDialog.cpp \
    NewGroupDialog.cpp \
    ScheduleCallDialog.cpp \
    UserData.cpp \
    UserSearchDialog.cpp \
    UserWidget.cpp \
    VideoMemberWidget.cpp \
    VideoWSClient.cpp \
    WSClient.cpp \
    calldialog.cpp \
    callwindow.cpp \
    groupwidget.cpp \
    main.cpp \
    mainwindow.cpp \
    messagewidget.cpp

HEADERS += \
    AudioWSClient.h \
    CallWidget.h \
    EventHandler.h \
    MembersListDialog.h \
    NewGroupDialog.h \
    ScheduleCallDialog.h \
    UI_UpdateTypes.h \
    UserData.h \
    UserSearchDialog.h \
    UserWidget.h \
    VideoMemberWidget.h \
    VideoWSClient.h \
    WSClient.h \
    calldialog.h \
    callwindow.h \
    groupwidget.h \
    mainwindow.h \
    messagewidget.h

FORMS += \
    CallWidget.ui \
    MembersListDialog.ui \
    NewGroupDialog.ui \
    ScheduleCallDialog.ui \
    UserSearchDialog.ui \
    UserWidget.ui \
    VideoMemberWidget.ui \
    calldialog.ui \
    callwindow.ui \
    groupwidget.ui \
    mainwindow.ui \
    messagewidget.ui

# Default rules for deployment.
qnx: target.path = /tmp/$${TARGET}/bin
else: unix:!android: target.path = /opt/$${TARGET}/bin
!isEmpty(target.path): INSTALLS += target

DISTFILES += \
    style.qss

RESOURCES += \
    resources.qrc
