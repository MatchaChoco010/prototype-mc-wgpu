use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use tokio::sync::mpsc::UnboundedSender;
use winit::event::*;

#[derive(Debug)]
pub enum MinecraftEvent {
    Draw,
    WinitEvent(Option<Event<'static, ()>>),
}

static EVENT_BUS: Lazy<Arc<Mutex<Option<UnboundedSender<MinecraftEvent>>>>> =
    Lazy::new(|| Arc::new(Mutex::new(None)));

pub fn set_tx(tx: UnboundedSender<MinecraftEvent>) {
    *EVENT_BUS.lock().unwrap() = Some(tx);
}

pub fn send(event: MinecraftEvent) {
    if let Some(tx) = EVENT_BUS.lock().unwrap().as_ref() {
        if let Err(e) = tx.send(event) {
            eprintln!("{:?}", e);
        }
    }
}
