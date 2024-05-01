#include "ui_callwindow.h"

#include <QAudioDevice>
#include <QAudioInput>
#include <QAudioOutput>
#include <QBuffer>
#include <QCamera>
#include <QCameraDevice>
#include <QIODevice>
#include <QLabel>
#include <QMediaDevices>
#include <QMediaRecorder>
#include <QVideoSink>
#include <UserData.h>
#include <VideoWSClient.h>
#include <callwindow.h>
#include <VideoMemberWidget.h>

CallWindow::CallWindow(QWidget *parent, QJsonObject* eventData)
    : QDialog(parent)
    , ui(new Ui::CallWindow)
{
    ui->setupUi(this);

    setWindowFlags(Qt::Window
                   | Qt::WindowMinimizeButtonHint
                   | Qt::WindowMaximizeButtonHint);

    userIdBin = UserData::getInstance()->getId().toUtf8();

    player = new QMediaPlayer(this);
    videoWidget = new QVideoWidget(this);
    session = new QMediaCaptureSession(this);
    recorder = new QMediaRecorder(this);

    QString channel = eventData->value("channel").toString();

    QUrl url;
    url.setScheme("ws");
    url.setHost("localhost");
    url.setPort(8081);
    url.setPath(QString("/video/%1/%2").arg(channel, UserData::getInstance()->getId()));

    client = new VideoWSClient(url, true, this);

    connect(client, &VideoWSClient::addNewVideoWidget, this, &CallWindow::onNewVideoWidget);
    connect(client, &VideoWSClient::addNewVideoWidgets, this, &CallWindow::onNewVideoWidgets);
    connect(client, &VideoWSClient::removeVideoWidget, this, &CallWindow::onRemovingVideoWidget);
    connect(client, &VideoWSClient::updateFrame, this, &CallWindow::onUpdateFrame);

    const QList<QCameraDevice> cameras = QMediaDevices::videoInputs();

    if (cameras.empty())
        qDebug() << "No cameras detected!";
    else {
        QCamera* camera = new QCamera(cameras[0]);
        session->setCamera(camera);

        connect(camera, &QCamera::errorOccurred, this, [this]() {
            qDebug() << "Error occured. Camera might be used by another process";
            session->setCamera(nullptr);
            player->setSource(QUrl("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"));
            player->setVideoOutput(videoWidget);
            player->setAudioOutput(new QAudioOutput(QMediaDevices::defaultAudioOutput()));
            player->play();
        });
    }

    if (session->camera())
        session->setVideoOutput(videoWidget);

    VideoMemberWidget *videoMemberWidget = new VideoMemberWidget(this, UserData::getInstance()->getUsername(), videoWidget);

    ui->videoGridLayout->addWidget(videoMemberWidget);

    if (session->camera())
        session->camera()->start();

    const QList<QAudioDevice> audioInputDevices = QMediaDevices::audioInputs();

    if (audioInputDevices.empty())
        qDebug() << "No audio inputs detected!";
    else {
        QAudioInput* audioInput = new QAudioInput(audioInputDevices[0], this);
        session->setAudioInput(audioInput);
    }

    const QList<QAudioDevice> audioOutputDevices = QMediaDevices::audioOutputs();

    if (audioInputDevices.empty())
        qDebug() << "No audio outputs detected!";
    else {
        QAudioOutput* audioOutput = new QAudioOutput(audioOutputDevices[0], this);
        session->setAudioOutput(audioOutput);
    }

    if (session->camera())
        connect(session->videoSink(), &QVideoSink::videoFrameChanged, this, &CallWindow::processFrame);
    else
        connect(player->videoSink(), &QVideoSink::videoFrameChanged, this, &CallWindow::processFrame);
}

CallWindow::~CallWindow()
{
    if (session->camera()) {
        session->camera()->stop();
        delete session->camera();
    }
    player->stop();
    delete player;
    delete session;
    delete ui;
}

void CallWindow::onNewVideoWidget(QString username)
{
    QVideoWidget* newVideoWidget = new QVideoWidget(this);

    VideoMemberWidget* videoMemberWidget = new VideoMemberWidget(this, username, newVideoWidget, VideoType::EXTERN);

    ui->videoGridLayout->addWidget(videoMemberWidget);

    videoWidgets[username] = videoMemberWidget;
}

void CallWindow::onNewVideoWidgets(QJsonArray members)
{
    for(int i = 0;i < members.count(); i++) {
        QVideoWidget* newVideoWidget = new QVideoWidget(this);

        VideoMemberWidget* videoMemberWidget = new VideoMemberWidget(this, members.at(i).toString(), newVideoWidget, VideoType::EXTERN);

        ui->videoGridLayout->addWidget(videoMemberWidget);

        videoWidgets[members.at(i).toString()] = videoMemberWidget;
    }
}

void CallWindow::onRemovingVideoWidget(QString username)
{
    if (videoWidgets.contains(username)) {
        qDebug() << "Removing 1 video widget.";
        ui->videoGridLayout->removeWidget(videoWidgets[username]);
        delete videoWidgets[username];
        videoWidgets.remove(username);
    }
}

void CallWindow::onUpdateFrame(QString userId, QByteArray frameData)
{
    videoWidgets[userId]->updateFrame(frameData);
}

void CallWindow::on_toggleVideoButton_clicked()
{
    if(session->camera()) {
        if (session->camera()->isActive()) {
            session->camera()->stop();
        }
        else {
            session->camera()->start();
        }
    }
}


void CallWindow::on_leaveCallButton_clicked()
{
    recorder->stop();
    delete this;
}

void CallWindow::processFrame(const QVideoFrame &frame)
{
    QImage image = frame.toImage();
    QByteArray arr;
    QBuffer buffer(&arr);
    buffer.open(QIODevice::WriteOnly);
    image.save(&buffer, "JPG");

    buffer.buffer().prepend(this->userIdBin);

    client->sendFrame(buffer.data());
}

