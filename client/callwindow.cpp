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
#include <QAudioSink>
#include <QAudioSource>

#if QT_CONFIG(permissions)
    #include <QCoreApplication>
    #include <QPermission>
#endif

CallWindow::CallWindow(QWidget *parent, QJsonObject* eventData, QList<QJsonObject> members)
    : QDialog(parent)
    , ui(new Ui::CallWindow)
{
    ui->setupUi(this);

    this->groupId = eventData->value("_id").toString();

    for (int i = 0; i < members.count(); ++i) {
        this->membersData[members.at(i)["_id"].toString()] = members.at(i)["username"].toString();
    }

    setWindowFlags(Qt::Window
                   | Qt::WindowMinimizeButtonHint
                   | Qt::WindowMaximizeButtonHint);

    userIdBin = UserData::getInstance()->getId().toUtf8();

    player = new QMediaPlayer(this);
    videoWidget = new QVideoWidget(this);
    session = new QMediaCaptureSession(this);

    QString channel = eventData->value("channel").toString();
    QString id = eventData->value("callId").toString();

    initVideoClient(channel, id);
    initCamera();
    initAudioClient(channel, id);
    checkPermissions();

    const QList<QAudioDevice> audioInputDevices = QMediaDevices::audioInputs();

    if (audioInputDevices.empty()) {
        qDebug() << "No audio inputs detected!";
        return;
    }

    qDebug() << audioInputDevices.size();

    const QAudioDevice &defaultAudioInputDevice = QMediaDevices::defaultAudioInput();
    ui->comboBoxAudioInputs->addItem(defaultAudioInputDevice.description(), QVariant::fromValue(defaultAudioInputDevice));

    for(const QAudioDevice &device : audioInputDevices)
        if (device != defaultAudioInputDevice)
            ui->comboBoxAudioInputs->addItem(device.description(), QVariant::fromValue(device));

    connect(ui->comboBoxAudioInputs, &QComboBox::activated, this, &CallWindow::handleInputDeviceChange);

    initAudioInput(defaultAudioInputDevice);
    initAudioOutput();

    if (session->audioInput() != nullptr)
        session->audioInput()->isMuted() ? ui->audioToggleButton->setText("Unmute") : ui->audioToggleButton->setText("Mute");

    ui->comboBoxAudioInputs->hide();

    ui->audioToggleButton->setCursor(Qt::PointingHandCursor);
    ui->toggleVideoButton->setCursor(Qt::PointingHandCursor);
    ui->leaveCallButton->setCursor(Qt::PointingHandCursor);

    ui->leaveCallButton->setStyleSheet("QPushButton { background-color: white; border: 2px solid #c61e33; border-radius: 10px; } QPushButton:hover { color: white; background-color: #c61e33;  }");
}

void CallWindow::initVideoClient(QString channel, QString id)
{
    QUrl urlVideo;
    urlVideo.setScheme("ws");
    urlVideo.setHost("localhost");
    urlVideo.setPort(8081);
    urlVideo.setPath(QString("/video/%1/%2/%3/%4").arg(channel,
                                                       id,
                                                       UserData::getInstance()->getId(),
                                                       UserData::getInstance()->getUsername()));

    videoClient = new VideoWSClient(urlVideo, true, this);

    connect(videoClient, &VideoWSClient::addNewVideoWidget, this, &CallWindow::onNewVideoWidget);
    connect(videoClient, &VideoWSClient::addNewVideoWidgets, this, &CallWindow::onNewVideoWidgets);
    connect(videoClient, &VideoWSClient::removeVideoWidget, this, &CallWindow::onRemovingVideoWidget);
    connect(videoClient, &VideoWSClient::updateFrame, this, &CallWindow::onUpdateFrame);
}

void CallWindow::initCamera()
{
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
            player->play();
            ui->toggleVideoButton->setDisabled(true);
            mockMode = true;
        });
    }

    if (session->camera())
        session->setVideoOutput(videoWidget);

    VideoMemberWidget *videoMemberWidget = new VideoMemberWidget(this, UserData::getInstance()->getUsername(), videoWidget);

    ui->videoGridLayout->addWidget(videoMemberWidget);

    if (session->camera())
        session->camera()->start();

    if (session->camera())
        connect(session->videoSink(), &QVideoSink::videoFrameChanged, this, &CallWindow::processFrame);
    else
        connect(player->videoSink(), &QVideoSink::videoFrameChanged, this, &CallWindow::processFrame);
}

void CallWindow::initAudioClient(QString channel, QString id)
{
    QUrl urlAudio;
    urlAudio.setScheme("ws");
    urlAudio.setHost("localhost");
    urlAudio.setPort(8082);
    urlAudio.setPath(QString("/audio/%1/%2/%3").arg(channel, id, UserData::getInstance()->getId()));

    audioClient = new AudioWSClient(urlAudio, true, this);

    connect(audioClient, &AudioWSClient::updateAudio, this, &CallWindow::onUpdateAudio);
}

void CallWindow::checkPermissions()
{
#if QT_CONFIG(permissions)
    QMicrophonePermission microphonePermission;
    switch (qApp->checkPermission(microphonePermission)) {
    case Qt::PermissionStatus::Undetermined:
        qApp->requestPermission(microphonePermission, this, &CallWindow::checkPermissions);
        return;
    case Qt::PermissionStatus::Denied:
        qDebug() << "Microphone permission not granted!";
        return;
    case Qt::PermissionStatus::Granted:
        qDebug() << "Microphone permission granted!";
        break;
    }
#endif
}

void CallWindow::initAudioInput(const QAudioDevice &device)
{
    if (mockMode) {
        ui->audioToggleButton->setDisabled(true);
        return;
    }

    QAudioInput* audioInput = new QAudioInput(device, this);

    if(!audioInput) {
        qDebug() << "Could not create new Audio Input!";
        return;
    }

    session->setAudioInput(audioInput);

    if (!session->audioInput()) {
        qDebug() << "Session audio input is null!";
        return;
    }

    session->audioInput()->setMuted(true);

    QAudioFormat formatInput = device.preferredFormat();

    if(!formatInput.isValid()) {
        qDebug() << "Invalid input audio format!";
        return;
    }

    audioSource = new QAudioSource(device, formatInput);

    if(!audioSource) {
        qDebug() << "Failed to create audio source!";
        return;
    }

    //connect(audioSource, &QAudioSource::stateChanged, this, &CallWindow::handleAudioState);
    inputSource = audioSource->start();

    if (!inputSource) {
        qDebug() << "Could not start reading input from microphone!";
        return;
    }

    static const qint64 bufferSizeAudio = 32768;//16384;

    connect(inputSource, &QIODevice::readyRead, [this]() {
        const qint64 len = qMin(audioSource->bytesAvailable(), bufferSizeAudio);

        QByteArray buffer(len, 0);
        qint64 bytesRead = inputSource->read(buffer.data(), len);

        if (bytesRead > 0 && !session->audioInput()->isMuted()) {
            //qDebug() << "Reading bytes! " << bytesRead;
            processSamples(buffer);
        }
        // else
        //     qDebug() << "Muted or no bytes!";
    });
}

void CallWindow::initAudioOutput()
{
    const QList<QAudioDevice> audioOutputDevices = QMediaDevices::audioOutputs();

    if (audioOutputDevices.empty()) {
        qDebug() << "No audio outputs detected!";
        return;
    }
    // else {
    //     QAudioOutput* audioOutput = new QAudioOutput(audioOutputDevices[0], this);
    //     session->setAudioOutput(audioOutput);
    // }

    QAudioFormat formatOutput = QMediaDevices::defaultAudioOutput().preferredFormat();

    if(!formatOutput.isValid()) {
        qDebug() << "Invalid output audio format!";
        return;
    }

    audioSink = new QAudioSink(QMediaDevices::defaultAudioOutput(), formatOutput);

    if (!audioSink) {
        qDebug() << "Could not create audio sink!";
        return;
    }

    audioOutputDevice = audioSink->start();
}

CallWindow::~CallWindow()
{
    if (session->camera()) {
        session->camera()->stop();
        delete session->camera();
    }
    if (audioSource && !mockMode)
        audioSource->stop();
    if (player) {
        player->stop();
        player->setVideoOutput(nullptr);
        delete player;
    }
    if (session)
        delete session;
    delete ui;
}

QString CallWindow::getGroupId()
{
    return this->groupId;
}

void CallWindow::updateMembersData(QList<QJsonObject> members)
{
    qDebug() << members;
}

void CallWindow::onNewVideoWidget(QString userId, QString username)
{
    VideoMemberWidget* videoMemberWidget = new VideoMemberWidget(this, userId, nullptr, VideoType::EXTERN);

    videoMemberWidget->updateUsername(username);

    ui->videoGridLayout->addWidget(videoMemberWidget);

    videoWidgets[userId] = videoMemberWidget;
}

void CallWindow::onNewVideoWidgets(QJsonArray members, QJsonObject usernames)
{
    for(int i = 0;i < members.count(); i++) {
        VideoMemberWidget* videoMemberWidget = new VideoMemberWidget(this, members.at(i).toString(), nullptr, VideoType::EXTERN);

        videoMemberWidget->updateUsername(usernames[members.at(i).toString()].toString());

        ui->videoGridLayout->addWidget(videoMemberWidget);

        videoWidgets[members.at(i).toString()] = videoMemberWidget;
    }
}

void CallWindow::onRemovingVideoWidget(QString username)
{
    if (videoWidgets.contains(username)) {
        //qDebug() << "Removing 1 video widget.";
        ui->videoGridLayout->removeWidget(videoWidgets[username]);
        delete videoWidgets[username];
        videoWidgets.remove(username);
    }
}

void CallWindow::onUpdateFrame(QString userId, QByteArray frameData)
{
    if (userId != UserData::getInstance()->getId())
        videoWidgets[userId]->updateFrame(frameData);
}

void CallWindow::onUpdateAudio(QString userId, QByteArray audioData)
{
    if (audioData.isNull() || audioData.isEmpty())
        return;

    if (audioOutputDevice && userId != UserData::getInstance()->getId())
        audioOutputDevice->write(audioData);
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
    qDebug() << "Leaving call!";
    delete this;
}

void CallWindow::processFrame(const QVideoFrame &frame)
{
    QImage image = frame.toImage().convertToFormat(QImage::Format_RGB888);

    if (!image.isNull()) {
        image = image.scaled(image.width() / 2, image.height() / 2, Qt::KeepAspectRatio);
        QByteArray arr;
        QBuffer buffer(&arr);
        buffer.open(QIODevice::WriteOnly);
        image.save(&buffer, "JPG");

        buffer.buffer().prepend(this->userIdBin);
        //qDebug() << "Frame size regular: " << buffer.buffer().size();
        videoClient->sendFrame(buffer.data());
    }
}

void CallWindow::processSamples(QByteArray data)
{
    audioClient->sendSamples(data.prepend(this->userIdBin));
}

void CallWindow::handleInputDeviceChange(int index)
{
    session->setAudioInput(nullptr);
    audioSource->stop();
    audioSource->disconnect(this);

    initAudioInput(ui->comboBoxAudioInputs->itemData(index).value<QAudioDevice>());
}

void CallWindow::handleAudioState(QAudio::State newState)
{
    switch(newState) {

    case QAudio::ActiveState:
        qDebug() << "Audio active!";
        break;
    case QAudio::SuspendedState:
        qDebug() << "Audio suspended!";
        break;
    case QAudio::StoppedState:
        qDebug() << "Audio stopped!";
        break;
    case QAudio::IdleState:
        qDebug() << "Audio idle!";
        break;
    }
}


void CallWindow::on_audioToggleButton_clicked()
{
    session->audioInput()->setMuted(!session->audioInput()->isMuted());
    session->audioInput()->isMuted() ? ui->audioToggleButton->setText("Unmute") : ui->audioToggleButton->setText("Mute");
}
