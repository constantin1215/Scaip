#ifndef CALLWINDOW_H
#define CALLWINDOW_H

#include "qjsonobject.h"
#include <QDialog>
#include <QMediaPlayer>
#include <QVideoWidget>

namespace Ui {
class CallWindow;
}

class CallWindow : public QDialog
{
    Q_OBJECT

public:
    explicit CallWindow(QWidget *parent = nullptr, QJsonObject* eventData = nullptr);
    ~CallWindow();

private:
    Ui::CallWindow *ui;
    QMediaPlayer *player;
    QVideoWidget *vw;
};

#endif // CALLWINDOW_H
